package com.resumego.matching.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumego.job.JobDescriptionService;
import com.resumego.job.dto.JobDescriptionDTO;
import com.resumego.job.dto.ParsedJobDescriptionDTO;
import com.resumego.matching.dto.MatchDetails;
import com.resumego.matching.dto.MatchResponse;
import com.resumego.matching.entity.JobMatch;
import com.resumego.matching.mapper.JobMatchMapper;
import com.resumego.resume.dto.ResumeVersionDTO;
import com.resumego.resume.service.ResumeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 匹配管线服务：负责匹配流程的编排，包括数据加载、缓存幂等、调用核心算法、
 * 结果持久化与响应构建。
 *
 * <p>核心匹配算法（归一化、分项匹配、相似度计算、加权、排序/并列/缺失规则）
 * 由 {@link MatchingService} 负责。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MatchingPipelineService {

    public static final String ALGORITHM_VERSION = "v1.1.0";

    private final ResumeService resumeService;
    private final JobDescriptionService jobDescriptionService;
    private final JobMatchMapper jobMatchMapper;
    private final ObjectMapper objectMapper;
    private final MatchingService matchingService;

    /**
     * 执行并返回岗位匹配结果。
     * 相同输入重复请求返回已有结果（幂等）。
     */
    @Transactional
    public MatchResponse match(Long versionId, Long jdId) {
        // 1. 加载简历版本与 JD
        ResumeVersionDTO version = resumeService.getVersion(versionId);
        JobDescriptionDTO jd = jobDescriptionService.findById(jdId);
        if (jd == null) {
            throw new IllegalArgumentException("JD_NOT_FOUND");
        }
        if (!"succeeded".equals(jd.getParseStatus())) {
            throw new IllegalStateException("JD_NOT_PARSED");
        }

        // 2. 检查是否已有相同输入的匹配结果（幂等）
        String fingerprint = computeFingerprint(version, jd);
        JobMatch existing = findExisting(versionId, jdId, fingerprint);
        if (existing != null) {
            log.info("命中幂等缓存: versionId={}, jdId={}, matchId={}",
                    versionId, jdId, existing.getId());
            return toResponse(existing);
        }

        // 3. 从简历中提取数据
        Map<String, Object> content = version.content();
        ParsedJobDescriptionDTO parsed = jd.getParsed();

        List<String> resumeSkillList = extractSkillList(content);
        List<String> resumeExpText   = extractExperienceTexts(content);
        String       resumeEdu       = extractHighestDegree(content);
        int          graduateYear    = extractGraduationYear(content);
        List<String> resumeDutyTexts = extractDutyCandidateTexts(content);

        // 4. 调用核心匹配算法
        MatchingService.MatchResult result = matchingService.executeMatching(
                resumeSkillList, resumeExpText, resumeEdu, graduateYear, resumeDutyTexts, parsed);

        // 5. 持久化新结果
        JobMatch entity = new JobMatch();
        entity.setResumeVersionId(versionId);
        entity.setJobDescriptionId(jdId);
        entity.setAlgorithmVersion(ALGORITHM_VERSION);
        entity.setMatchScore(result.matchScore());
        entity.setDetailsJson(serializeDetails(result.details()));
        entity.setInputFingerprint(fingerprint);
        entity.setCreatedAt(LocalDateTime.now());

        jobMatchMapper.insert(entity);
        log.info("匹配完成: id={}, versionId={}, jdId={}, score={}",
                entity.getId(), versionId, jdId, result.matchScore());

        return MatchResponse.of(entity.getId(), result.matchScore(), result.details());
    }

    /**
     * 将一份简历与所有已解析 JD 进行匹配，返回匹配度最高的 topN 个结果。
     *
     * @param versionId 简历版本 ID
     * @param topN      返回前 N 个最高匹配结果
     * @return 按 matchScore 降序排列的匹配列表（含 JD 标题和公司名）
     */
    public List<BatchMatchResult> batchMatch(Long versionId) {
        resumeService.getVersion(versionId); // 校验简历存在

        List<JobDescriptionDTO> allJds = jobDescriptionService.findAllByUser();
        List<BatchMatchResult> results = new ArrayList<>();

        for (JobDescriptionDTO jd : allJds) {
            if (!"succeeded".equals(jd.getParseStatus())) continue;

            try {
                MatchResponse mr = match(versionId, jd.getId());
                results.add(new BatchMatchResult(
                        jd.getId(), mr.matchScore(), mr.details().getMatchLevel()));
            } catch (Exception e) {
                log.warn("batchMatch: 匹配 jdId={} 失败: {}", jd.getId(), e.getMessage());
            }
        }

        results.sort((a, b) -> Integer.compare(b.matchScore(), a.matchScore()));
        return results;
    }

    /** 批量匹配的单条结果。仅返回 ID + 分数，标题/公司由前端已有的 JD 列表回填。 */
    public record BatchMatchResult(
            Long jobDescriptionId,
            int matchScore,
            String matchLevel
    ) {}

    // ================================================================
    // 缓存与幂等
    // ================================================================

    /**
     * 计算匹配输入的 SHA-256 指纹。
     * 指纹 = SHA-256(算法版本 + 简历内容 JSON + JD 解析结果 JSON)。
     * 用于幂等判断：相同输入必然产生相同指纹，命中缓存直接返回。
     */
    private String computeFingerprint(ResumeVersionDTO version, JobDescriptionDTO jd) {
        try {
            String canonicalVersion = objectMapper.writeValueAsString(version.content());
            String canonicalParsed = jd.getParsed() != null
                    ? objectMapper.writeValueAsString(jd.getParsed())
                    : "";
            String input = ALGORITHM_VERSION + canonicalVersion + canonicalParsed;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("指纹计算失败", e);
        }
    }

    /**
     * 查询是否已有相同输入的匹配记录。
     * 匹配条件：同一简历版本 + 同一 JD + 同一算法版本 + 同一输入指纹。
     */
    private JobMatch findExisting(Long versionId, Long jdId, String fingerprint) {
        QueryWrapper<JobMatch> query = new QueryWrapper<>();
        query.eq("resume_version_id", versionId)
             .eq("job_description_id", jdId)
             .eq("algorithm_version", ALGORITHM_VERSION)
             .eq("input_fingerprint", fingerprint);
        return jobMatchMapper.selectOne(query, false);
    }

    // ================================================================
    // 序列化 / 反序列化
    // ================================================================

    /** 将 MatchDetails 序列化为 JSON 字符串存入数据库。 */
    private String serializeDetails(MatchDetails details) {
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("details 序列化失败", e);
        }
    }

    /** 将数据库实体转为接口响应对象。 */
    private MatchResponse toResponse(JobMatch entity) {
        MatchDetails details = parseDetails(entity.getDetailsJson());
        return MatchResponse.of(entity.getId(), entity.getMatchScore(), details);
    }

    /** 从 JSON 字符串反序列化 MatchDetails。解析失败返回空对象，不阻断主流程。 */
    private MatchDetails parseDetails(String json) {
        try {
            return objectMapper.readValue(json, MatchDetails.class);
        } catch (JsonProcessingException e) {
            log.error("details_json 反序列化失败", e);
            return new MatchDetails();
        }
    }

    // ================================================================
    // 简历数据提取
    // ================================================================

    /** 从简历 content 中提取技能列表。 */
    @SuppressWarnings("unchecked")
    private List<String> extractSkillList(Map<String, Object> content) {
        Object skills = content.get("skills");
        if (skills instanceof List<?> list) {
            return list.stream()
                    .filter(Objects::nonNull)
                    .map(Object::toString)
                    .collect(java.util.stream.Collectors.toList());
        }
        return Collections.emptyList();
    }

    /** 从简历 content 中提取项目/经历的标题和描述文本。 */
    @SuppressWarnings("unchecked")
    private List<String> extractExperienceTexts(Map<String, Object> content) {
        List<String> result = new ArrayList<>();
        Object projects = content.get("projects");
        if (projects instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> proj) {
                    String title = Objects.toString(proj.get("title"), "");
                    String desc  = Objects.toString(proj.get("description"), "");
                    if (!title.isBlank()) result.add(title);
                    if (!desc.isBlank()) result.add(desc);
                }
            }
        }
        return result;
    }

    /** 从简历 content 中提取最高学历。 */
    @SuppressWarnings("unchecked")
    private String extractHighestDegree(Map<String, Object> content) {
        Object education = content.get("education");
        if (education instanceof List<?> list && !list.isEmpty()) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> edu) {
                    Object degree = edu.get("degree");
                    if (degree != null && !degree.toString().isBlank()) {
                        return degree.toString().trim();
                    }
                }
            }
        }
        return null;
    }

    /** 从简历 education 的 period 字段提取毕业年份。未填返回 0。 */
    @SuppressWarnings("unchecked")
    private int extractGraduationYear(Map<String, Object> content) {
        Object education = content.get("education");
        if (education instanceof List<?> list && !list.isEmpty()) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> edu) {
                    Object period = edu.get("period");
                    if (period != null) {
                        String p = period.toString().trim();
                        // 格式: "2024-2028" → 取末尾年份
                        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d{4})\\s*[-~至到]\\s*(\\d{4})").matcher(p);
                        if (m.find()) return Integer.parseInt(m.group(2));
                        // 单年份: "2028"
                        m = java.util.regex.Pattern.compile("(\\d{4})").matcher(p);
                        if (m.find()) return Integer.parseInt(m.group(1));
                    }
                }
            }
        }
        return 0;
    }

    /**
     * 从简历 content 中提取用于职责覆盖维度匹配的文本片段列表。
     * 包含：技能名称、项目标题与描述、教育院校与专业。
     * 每段作为独立候选文本，供 n-gram 匹配寻找最佳重叠。
     */
    @SuppressWarnings("unchecked")
    private List<String> extractDutyCandidateTexts(Map<String, Object> content) {
        List<String> result = new ArrayList<>();
        // 技能名称
        result.addAll(extractSkillList(content));
        // 项目标题与描述
        result.addAll(extractExperienceTexts(content));
        // 教育院校与专业
        Object education = content.get("education");
        if (education instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> edu) {
                    String school = Objects.toString(edu.get("school"), "");
                    String major  = Objects.toString(edu.get("major"), "");
                    if (!school.isBlank()) result.add(school);
                    if (!major.isBlank())  result.add(major);
                }
            }
        }
        return result;
    }
}
