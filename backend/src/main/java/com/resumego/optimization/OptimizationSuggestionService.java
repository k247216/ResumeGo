package com.resumego.optimization;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumego.ai.AiClient;
import com.resumego.ai.AiClientSelector;
import com.resumego.ai.AiConfig;
import com.resumego.ai.AiErrorCategory;
import com.resumego.ai.AiInvocationMapper;
import com.resumego.ai.AiInvocationService;
import com.resumego.ai.AiRequest;
import com.resumego.ai.AiResult;
import com.resumego.ai.AiInvocation;
import com.resumego.ai.validate.AiOutputValidator;
import com.resumego.company.CompanyProfileService;
import com.resumego.common.CurrentUser;
import com.resumego.job.JobDescription;
import com.resumego.job.JobDescriptionMapper;
import com.resumego.assessment.dto.ResumeAssessmentResponse;
import com.resumego.assessment.service.ResumeAssessmentService;
import com.resumego.matching.dto.MatchResponse;
import com.resumego.matching.entity.JobMatch;
import com.resumego.matching.mapper.JobMatchMapper;
import com.resumego.matching.service.MatchingPipelineService;
import com.resumego.optimization.dto.GenerateSuggestionsResponse;
import com.resumego.optimization.dto.JobMatchResumeContent;
import com.resumego.optimization.dto.OptimizationSuggestionDTO;
import com.resumego.resume.dto.CapabilityEvidenceResponse;
import com.resumego.resume.dto.ResumeVersionDTO;
import com.resumego.resume.repository.CapabilityEvidenceRepository;
import com.resumego.resume.repository.ResumeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AI 优化建议服务。
 * <p>
 * 核心职责：
 * <ol>
 *   <li>接收 jobMatchId，查询关联的简历、JD、匹配缺口和证据</li>
 *   <li>构建 AI Prompt，调用 AI 模型生成优化建议</li>
 *   <li>校验 AI 返回的 JSON 结构</li>
 *   <li>将建议写入 optimization_suggestions 表</li>
 *   <li>写入 ai_invocations 审计日志</li>
 * </ol>
 * <p>
 * 安全约束：所有外部入参强制校验；SQL 使用参数化查询；密钥不硬编码；
 * 异常全量捕获；不打印隐私数据。
 */
@Service
public class OptimizationSuggestionService {

    private static final Logger log = LoggerFactory.getLogger(OptimizationSuggestionService.class);
    private static final String FEATURE_TYPE = "resume_optimize";

    private final JobMatchMapper jobMatchMapper;
    private final JobDescriptionMapper jobDescriptionMapper;
    private final ResumeRepository resumeRepository;
    private final CapabilityEvidenceRepository evidenceRepository;
    private final OptimizationSuggestionMapper suggestionMapper;
    private final AiInvocationMapper aiInvocationMapper;
    private final AiInvocationService aiInvocationService;
    private final AiClient aiClient;
    private final AiOutputValidator outputValidator;
    private final SuggestionPromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;
    private final CompanyProfileService companyProfileService;
    private final JdbcTemplate jdbcTemplate;
    private final MatchingPipelineService matchingPipelineService;
    private final ResumeAssessmentService resumeAssessmentService;
    private final AiConfig aiConfig;

    public OptimizationSuggestionService(
            JobMatchMapper jobMatchMapper,
            JobDescriptionMapper jobDescriptionMapper,
            ResumeRepository resumeRepository,
            CapabilityEvidenceRepository evidenceRepository,
            OptimizationSuggestionMapper suggestionMapper,
            AiInvocationMapper aiInvocationMapper,
            AiInvocationService aiInvocationService,
            AiClientSelector aiClientSelector,
            AiOutputValidator outputValidator,
            SuggestionPromptBuilder promptBuilder,
            ObjectMapper objectMapper,
            CompanyProfileService companyProfileService,
            JdbcTemplate jdbcTemplate,
            MatchingPipelineService matchingPipelineService,
            ResumeAssessmentService resumeAssessmentService,
            AiConfig aiConfig) {
        this.jobMatchMapper = jobMatchMapper;
        this.jobDescriptionMapper = jobDescriptionMapper;
        this.resumeRepository = resumeRepository;
        this.evidenceRepository = evidenceRepository;
        this.suggestionMapper = suggestionMapper;
        this.aiInvocationMapper = aiInvocationMapper;
        this.aiInvocationService = aiInvocationService;
        this.aiClient = aiClientSelector.getClient();
        this.outputValidator = outputValidator;
        this.promptBuilder = promptBuilder;
        this.objectMapper = objectMapper;
        this.companyProfileService = companyProfileService;
        this.jdbcTemplate = jdbcTemplate;
        this.matchingPipelineService = matchingPipelineService;
        this.resumeAssessmentService = resumeAssessmentService;
        this.aiConfig = aiConfig;
    }

    /**
     * 基于匹配结果生成 AI 优化建议。
     *
     * @param jobMatchId 匹配结果 ID（必须 > 0）
     * @return 生成的建议列表
     */
    @Transactional
    public GenerateSuggestionsResponse generateSuggestions(long jobMatchId) {
        // API Key 已配置则自动使用真实数据模式
        if (aiConfig.isApiKeyConfigured()) {
            return generateSuggestionsWithRealData(jobMatchId);
        }

        SuggestionValidation.requirePositive(jobMatchId, "jobMatchId");

        // 1. 查询匹配结果
        JobMatch jobMatch = jobMatchMapper.selectById(jobMatchId);
        if (jobMatch == null) {
            throw new IllegalArgumentException("匹配结果不存在");
        }

        long resumeVersionId = jobMatch.getResumeVersionId();
        long jobDescriptionId = jobMatch.getJobDescriptionId();

        // 2. 查询简历版本
        ResumeVersionDTO resumeVersion = resumeRepository.findVersionById(resumeVersionId);
        if (resumeVersion == null) {
            throw new IllegalArgumentException("简历版本不存在");
        }

        // 3. 查询 JD
        JobDescription jobDescription = jobDescriptionMapper.selectById(jobDescriptionId);
        if (jobDescription == null) {
            throw new IllegalArgumentException("岗位 JD 不存在");
        }
        if (jobDescription.getParsedJson() == null) {
            throw new IllegalStateException("岗位 JD 尚未解析完成，无法生成建议");
        }

        // 4. 查询关联的能力证据
        var resumeContent = buildResumeContent(resumeVersion);

        // 5. 解析 JD 和匹配缺口
        Map<String, Object> parsedJd = parseJsonMap(jobDescription.getParsedJson());
        List<Map<String, Object>> matchGaps = extractGaps(jobMatch.getDetailsJson());
        Map<String, Object> companyProfile = companyProfileService.findEnabledProfileByCompanyName(jobDescription.getCompanyName());

        // 6. 构建 AI Prompt
        String systemPrompt = promptBuilder.buildSystemPrompt();
        String userMessage = promptBuilder.buildUserMessageWithCompanyProfile(
                resumeContent, parsedJd, matchGaps, companyProfile);

        // 7-12. 调用 AI 并处理结果
        return invokeAiAndProcessResult(jobMatchId, resumeVersionId, systemPrompt, userMessage);
    }

    /**
     * 基于真实匹配+评分接口生成 AI 优化建议（真实模式）。
     * <p>
     * 与 {@link #generateSuggestions(long)} 的区别：
     * 使用 MatchingPipelineService 和 ResumeAssessmentService 返回的结构化数据，
     * 替代手工从 JobMatch 表解析的 raw JSON。
     *
     * @param jobMatchId 匹配结果 ID（必须 > 0）
     * @return 生成的建议列表
     */
    @Transactional
    public GenerateSuggestionsResponse generateSuggestionsWithRealData(long jobMatchId) {
        SuggestionValidation.requirePositive(jobMatchId, "jobMatchId");

        // 1. 查询匹配结果
        JobMatch jobMatch = jobMatchMapper.selectById(jobMatchId);
        if (jobMatch == null) {
            throw new IllegalArgumentException("匹配结果不存在");
        }

        long resumeVersionId = jobMatch.getResumeVersionId();
        long jobDescriptionId = jobMatch.getJobDescriptionId();

        // 2. 查询简历版本
        ResumeVersionDTO resumeVersion = resumeRepository.findVersionById(resumeVersionId);
        if (resumeVersion == null) {
            throw new IllegalArgumentException("简历版本不存在");
        }

        // 3. 查询 JD
        JobDescription jobDescription = jobDescriptionMapper.selectById(jobDescriptionId);
        if (jobDescription == null) {
            throw new IllegalArgumentException("岗位 JD 不存在");
        }
        if (jobDescription.getParsedJson() == null) {
            throw new IllegalStateException("岗位 JD 尚未解析完成，无法生成建议");
        }

        // 4. 查询关联的能力证据
        var resumeContent = buildResumeContent(resumeVersion);

        // 5. 调用真实匹配接口
        MatchResponse matchResponse;
        try {
            matchResponse = matchingPipelineService.match(resumeVersionId, jobDescriptionId);
        } catch (Exception e) {
            log.error("调用 MatchingPipelineService 失败: versionId={}, jdId={}", resumeVersionId, jobDescriptionId, e);
            throw new IllegalStateException("匹配服务调用失败，无法生成建议", e);
        }

        // 6. 调用真实评分接口
        ResumeAssessmentResponse assessmentResponse = null;
        try {
            assessmentResponse = resumeAssessmentService.assess(resumeVersionId);
        } catch (Exception e) {
            log.warn("调用 ResumeAssessmentService 失败，将在无评分数据下继续: versionId={}", resumeVersionId, e);
        }

        // 7. 解析 JD
        Map<String, Object> parsedJd = parseJsonMap(jobDescription.getParsedJson());
        Map<String, Object> companyProfile = companyProfileService.findEnabledProfileByCompanyName(jobDescription.getCompanyName());

        // 8. 构建 AI Prompt（真实模式）
        String systemPrompt = promptBuilder.buildSystemPrompt();
        String userMessage = promptBuilder.buildUserMessage(
                resumeContent, parsedJd, matchResponse, assessmentResponse, companyProfile);

        // 9. 调用 AI 并处理结果
        return invokeAiAndProcessResult(jobMatchId, resumeVersionId, systemPrompt, userMessage);
    }

    /**
     * 调用 AI 并处理结果（公共逻辑）。
     * Mock 和真实模式共享此方法。
     */
    private GenerateSuggestionsResponse invokeAiAndProcessResult(
            long jobMatchId, long resumeVersionId, String systemPrompt, String userMessage) {
        String requestId = UUID.randomUUID().toString();
        AiRequest aiRequest = AiRequest.builder()
                .requestId(requestId)
                .featureType(FEATURE_TYPE)
                .userId(CurrentUser.DEMO_USER_ID)
                .promptVersion(SuggestionPromptBuilder.PROMPT_VERSION)
                .systemPrompt(systemPrompt)
                .userMessage(userMessage)
                .build();

        long startTime = System.currentTimeMillis();
        AiResult aiResult;
        try {
            aiResult = aiClient.invoke(aiRequest);
        } catch (Exception e) {
            log.error("AI 调用异常: requestId={}", requestId, e);
            aiResult = AiResult.failure(requestId, AiErrorCategory.PROVIDER_ERROR,
                    "AI 服务异常，请稍后重试", System.currentTimeMillis() - startTime);
        }

        // 8. 校验 AI 输出
        boolean schemaValid = false;
        List<Map<String, Object>> rawSuggestions = new ArrayList<>();

        if (aiResult.success() && aiResult.content() != null) {
            try {
                String jsonContent = outputValidator.extractJson(aiResult.content());
                var jsonResult = outputValidator.validateJson(jsonContent);
                if (jsonResult.isValid()) {
                    var fieldResult = outputValidator.validateRequiredFields(jsonContent,
                            List.of("suggestions"));
                    if (fieldResult.isValid()) {
                        var arrayResult = outputValidator.validateFieldType(jsonContent,
                                "suggestions", "array");
                        if (arrayResult.isValid()) {
                            schemaValid = true;
                            rawSuggestions = parseSuggestionsArray(jsonContent);
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("AI 输出校验异常: requestId={}", requestId, e);
            }
        }

        // 9. 写入 AI 审计日志
        aiInvocationService.logInvocationWithSchema(aiRequest, aiResult, schemaValid);

        // 获取刚写入的审计记录 ID
        AiInvocation invocation = findInvocationByRequestId(requestId);
        if (invocation == null) {
            throw new IllegalStateException("AI 审计日志写入失败");
        }

        // 10. 如果 AI 失败或校验失败，不创建建议
        if (!schemaValid || rawSuggestions.isEmpty()) {
            log.warn("AI 建议生成失败: requestId={}, schemaValid={}, count={}",
                    requestId, schemaValid, rawSuggestions.size());
            return new GenerateSuggestionsResponse(List.of());
        }

        // 11. 校验建议数量上限
        SuggestionValidation.validateSuggestionsCount(rawSuggestions.size());

        // 12. 遍历并创建建议
        List<OptimizationSuggestionDTO> created = new ArrayList<>();
        int failedCount = 0;
        for (int i = 0; i < rawSuggestions.size(); i++) {
            try {
                OptimizationSuggestionDTO dto = createSuggestion(
                        jobMatchId, resumeVersionId, invocation.getId(), rawSuggestions.get(i));
                created.add(dto);
            } catch (Exception e) {
                failedCount++;
                log.warn("创建建议失败: index={}", i, e);
            }
        }

        log.info("建议生成完成: requestId={}, created={}, failed={}",
                requestId, created.size(), failedCount);
        return new GenerateSuggestionsResponse(created);
    }

    /**
     * 获取已生成的建议列表。
     */
    public GenerateSuggestionsResponse getSuggestions(long jobMatchId) {
        SuggestionValidation.requirePositive(jobMatchId, "jobMatchId");

        QueryWrapper<OptimizationSuggestion> query = new QueryWrapper<>();
        query.eq("job_match_id", jobMatchId)
                .orderByAsc("id");

        List<OptimizationSuggestion> entities = suggestionMapper.selectList(query);
        List<OptimizationSuggestionDTO> dtos = entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());

        return new GenerateSuggestionsResponse(dtos);
    }

    /**
     * 采纳建议：创建新简历版本并应用建议内容。
     */
    @Transactional
    public void acceptSuggestion(long suggestionId) {
        SuggestionValidation.requirePositive(suggestionId, "suggestionId");

        OptimizationSuggestion suggestion = suggestionMapper.selectById(suggestionId);
        if (suggestion == null) {
            throw new IllegalArgumentException("建议不存在");
        }
        if (!"pending".equals(suggestion.getStatus())) {
            throw new IllegalStateException("建议状态不允许采纳: " + suggestion.getStatus());
        }
        if (suggestion.getSuggestedText() == null) {
            throw new IllegalStateException("无证据建议不可采纳");
        }

        // 1. 读取当前简历版本
        long resumeVersionId = suggestion.getResumeVersionId();
        ResumeVersionDTO currentVersion = resumeRepository.findVersionById(resumeVersionId);
        if (currentVersion == null) {
            throw new IllegalStateException("简历版本不存在");
        }
        long resumeId = currentVersion.resumeId();

        // 2. 应用 suggestedText 到 content_json
        String contentJsonStr;
        try {
            contentJsonStr = objectMapper.writeValueAsString(currentVersion.content());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("序列化简历内容失败", e);
        }
        String newContentJson = resumeRepository.applySuggestion(
                contentJsonStr,
                suggestion.getSectionKey(),
                suggestion.getSuggestedText()
        );

        // 3. 创建新版本
        int newVersionNo = resumeRepository.findMaxVersionNo(resumeId) + 1;
        long newVersionId = resumeRepository.insertVersion(
                resumeId,
                currentVersion.id(),
                newVersionNo,
                newContentJson,
                "AI 建议: " + suggestion.getReasonText(),
                "ai_suggestion"
        );

        // 4. 更新简历当前版本
        resumeRepository.updateCurrentVersionId(resumeId, newVersionId);

        // 5. 更新建议记录
        suggestion.setStatus("accepted");
        suggestion.setAcceptedVersionId(newVersionId);
        suggestion.setDecidedAt(LocalDateTime.now());
        suggestionMapper.updateById(suggestion);

        log.info("建议已采纳: suggestionId={}, newVersionId={}, versionNo=v{}",
                suggestionId, newVersionId, newVersionNo);
    }

    /**
     * 拒绝建议。pending 和 high_risk 状态允许拒绝。
     */
    @Transactional
    public void rejectSuggestion(long suggestionId) {
        SuggestionValidation.requirePositive(suggestionId, "suggestionId");

        OptimizationSuggestion suggestion = suggestionMapper.selectById(suggestionId);
        if (suggestion == null) {
            throw new IllegalArgumentException("建议不存在");
        }
        String status = suggestion.getStatus();
        if (!"pending".equals(status) && !"high_risk".equals(status)) {
            throw new IllegalStateException("建议状态不允许拒绝: " + status);
        }

        suggestion.setStatus("rejected");
        suggestion.setDecidedAt(LocalDateTime.now());
        suggestionMapper.updateById(suggestion);

        log.info("建议已拒绝: suggestionId={}", suggestionId);
    }

    // ── 私有辅助方法 ──

    private JobMatchResumeContent buildResumeContent(ResumeVersionDTO resumeVersion) {
        if (resumeVersion.content() == null) {
            throw new IllegalStateException("简历版本内容为空，无法生成建议");
        }
        List<Long> evidenceIds = queryEvidenceRefs(resumeVersion.id());
        List<JobMatchResumeContent.CapabilityEvidenceInfo> evidences = new ArrayList<>();

        for (Long evidenceId : evidenceIds) {
            evidenceRepository.findActiveByIdAndUserId(evidenceId, CurrentUser.DEMO_USER_ID)
                    .ifPresent(e -> evidences.add(
                            new JobMatchResumeContent.CapabilityEvidenceInfo(
                                    e.id(), e.evidenceType(), e.title(),
                                    e.situation(), e.actionText(), e.resultText(),
                                    e.skillTags()
                            )
                    ));
        }

        return new JobMatchResumeContent(resumeVersion.content(), evidences);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonMap(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            log.warn("JSON 解析失败", e);
            return Map.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractGaps(String detailsJson) {
        try {
            Map<String, Object> details = objectMapper.readValue(detailsJson, Map.class);
            Object gaps = details.get("gaps");
            if (gaps instanceof List) {
                return (List<Map<String, Object>>) gaps;
            }
        } catch (Exception e) {
            log.warn("匹配缺口解析失败", e);
        }
        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> parseSuggestionsArray(String jsonContent) {
        try {
            Map<String, Object> root = objectMapper.readValue(jsonContent, Map.class);
            Object suggestions = root.get("suggestions");
            if (suggestions instanceof List) {
                return (List<Map<String, Object>>) suggestions;
            }
        } catch (Exception e) {
            log.warn("suggestions 数组解析失败", e);
        }
        return List.of();
    }

    /**
     * 创建单条建议记录。所有外部入参（来自 AI 输出的 raw Map）均经过严格校验。
     */
    private OptimizationSuggestionDTO createSuggestion(
            long jobMatchId, long resumeVersionId, long aiInvocationId,
            Map<String, Object> raw) {

        // ── 提取并校验每个字段 ──

        String sectionKey = getStringField(raw, "sectionKey");
        SuggestionValidation.validateSectionKey(sectionKey);

        String originalText = getStringField(raw, "originalText");
        SuggestionValidation.validateTextField(originalText, "originalText",
                SuggestionValidation.MAX_TEXT_LENGTH);

        String reasonText = getStringField(raw, "reason");
        SuggestionValidation.validateTextField(reasonText, "reason",
                SuggestionValidation.MAX_REASON_LENGTH);

        String targetRequirement = getStringField(raw, "targetRequirement");
        SuggestionValidation.validateTextField(targetRequirement, "targetRequirement",
                SuggestionValidation.MAX_REASON_LENGTH);

        String confidence = getStringField(raw, "confidence");
        SuggestionValidation.validateConfidence(confidence);

        // ── 提取 evidenceId 和 suggestedText ──
        Object evidenceIdObj = raw.get("evidenceId");
        Long evidenceId = (evidenceIdObj instanceof Number)
                ? ((Number) evidenceIdObj).longValue() : null;

        Object suggestedTextObj = raw.get("suggestedText");
        String suggestedText = (suggestedTextObj instanceof String)
                ? (String) suggestedTextObj : null;

        // ── 业务规则校验 ──
        String status;
        String riskLevel = null;
        if (evidenceId == null) {
            // 无证据：强制 status = evidence_required，suggestedText 必须为 null
            status = "evidence_required";
            suggestedText = null;
        } else {
            // 有证据：校验证据存在且未被删除
            CapabilityEvidenceResponse evidence = validateEvidenceExists(evidenceId);

            status = "pending";
            if (suggestedText == null || suggestedText.isBlank()) {
                throw new IllegalArgumentException("有证据时 suggestedText 不能为空");
            }
            if (suggestedText.length() > SuggestionValidation.MAX_TEXT_LENGTH) {
                throw new IllegalArgumentException(
                        "suggestedText 长度不能超过 " + SuggestionValidation.MAX_TEXT_LENGTH);
            }

            // ── 编造事实检测（方案B：标记 high_risk，仍保存）──
            Set<String> evidenceWords = SuggestionValidation.extractEvidenceKeywords(
                    evidence.actionText(), evidence.resultText(), evidence.skillTags());
            if (SuggestionValidation.detectFabrication(suggestedText, evidenceWords)) {
                status = "high_risk";
                riskLevel = "high";
                log.warn("检测到疑似编造事实: evidenceId={}, suggestedText前50字符={}",
                        evidenceId, suggestedText.substring(0, Math.min(50, suggestedText.length())));
            }
        }

        // ── 持久化 ──
        OptimizationSuggestion entity = new OptimizationSuggestion();
        entity.setJobMatchId(jobMatchId);
        entity.setResumeVersionId(resumeVersionId);
        entity.setEvidenceId(evidenceId);
        entity.setSectionKey(sectionKey);
        entity.setOriginalText(originalText);
        entity.setSuggestedText(suggestedText);
        entity.setReasonText(reasonText);
        entity.setTargetRequirement(targetRequirement);
        entity.setStatus(status);
        entity.setRiskLevel(riskLevel);
        entity.setPromptVersion(SuggestionPromptBuilder.PROMPT_VERSION);
        entity.setGenerationAiInvocationId(aiInvocationId);
        entity.setCreatedAt(LocalDateTime.now());

        suggestionMapper.insert(entity);

        return toDto(entity);
    }

    /**
     * 校验 evidenceId 对应的证据存在且未被软删除。
     *
     * @return 证据对象（用于后续编造检测）
     */
    private CapabilityEvidenceResponse validateEvidenceExists(long evidenceId) {
        SuggestionValidation.requirePositive(evidenceId, "evidenceId");

        return evidenceRepository
                .findActiveByIdAndUserId(evidenceId, CurrentUser.DEMO_USER_ID)
                .orElseThrow(() -> new IllegalArgumentException(
                        "证据不存在或已删除: evidenceId=" + evidenceId));
    }

    private String getStringField(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return (value instanceof String) ? (String) value : null;
    }

    private AiInvocation findInvocationByRequestId(String requestId) {
        QueryWrapper<AiInvocation> query = new QueryWrapper<>();
        query.eq("request_id", requestId);
        return aiInvocationMapper.selectOne(query);
    }

    private List<Long> queryEvidenceRefs(long resumeVersionId) {
        return jdbcTemplate.query(
                "SELECT evidence_id FROM resume_evidence_refs WHERE resume_version_id = ?",
                (rs, rowNum) -> rs.getLong("evidence_id"),
                resumeVersionId
        );
    }

    private OptimizationSuggestionDTO toDto(OptimizationSuggestion entity) {
        return new OptimizationSuggestionDTO(
                entity.getId(),
                entity.getJobMatchId(),
                entity.getResumeVersionId(),
                entity.getEvidenceId(),
                entity.getSectionKey(),
                entity.getOriginalText(),
                entity.getSuggestedText(),
                entity.getReasonText(),
                entity.getTargetRequirement(),
                entity.getStatus(),
                entity.getRiskLevel(),
                entity.getCreatedAt()
        );
    }
}
