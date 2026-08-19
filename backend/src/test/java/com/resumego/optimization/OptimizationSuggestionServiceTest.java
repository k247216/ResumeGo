package com.resumego.optimization;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumego.ai.AiClient;
import com.resumego.ai.AiClientSelector;
import com.resumego.ai.AiConfig;
import com.resumego.ai.AiInvocation;
import com.resumego.ai.AiInvocationMapper;
import com.resumego.ai.AiInvocationService;
import com.resumego.ai.validate.AiOutputValidator;
import com.resumego.assessment.service.ResumeAssessmentService;
import com.resumego.company.CompanyProfileService;
import com.resumego.job.JobDescription;
import com.resumego.job.JobDescriptionMapper;
import com.resumego.matching.entity.JobMatch;
import com.resumego.matching.mapper.JobMatchMapper;
import com.resumego.matching.service.MatchingPipelineService;
import com.resumego.optimization.dto.GenerateSuggestionsResponse;
import com.resumego.optimization.dto.OptimizationSuggestionDTO;
import com.resumego.resume.dto.ResumeVersionDTO;
import com.resumego.resume.dto.CapabilityEvidenceResponse;
import com.resumego.resume.repository.CapabilityEvidenceRepository;
import com.resumego.resume.repository.ResumeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * OptimizationSuggestionService 单元测试。
 * 使用 Mockito 模拟所有外部依赖（Mapper、Repository、AI Client 等），
 * 不依赖 Spring 上下文，符合项目规范。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OptimizationSuggestionService 单元测试")
class OptimizationSuggestionServiceTest {

    @Mock
    private JobMatchMapper jobMatchMapper;
    @Mock
    private JobDescriptionMapper jobDescriptionMapper;
    @Mock
    private ResumeRepository resumeRepository;
    @Mock
    private CapabilityEvidenceRepository evidenceRepository;
    @Mock
    private OptimizationSuggestionMapper suggestionMapper;
    @Mock
    private AiInvocationMapper aiInvocationMapper;
    @Mock
    private AiInvocationService aiInvocationService;
    @Mock
    private AiClient aiClient;
    @Mock
    private AiOutputValidator outputValidator;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private CompanyProfileService companyProfileService;

    @Mock
    private AiClientSelector aiClientSelector;
    @Mock
    private MatchingPipelineService matchingPipelineService;
    @Mock
    private ResumeAssessmentService resumeAssessmentService;
    @Mock
    private AiConfig aiConfig;

    private SuggestionPromptBuilder promptBuilder;
    private ObjectMapper objectMapper;
    private OptimizationSuggestionService suggestionService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        outputValidator = new AiOutputValidator(objectMapper);
        promptBuilder = new SuggestionPromptBuilder(objectMapper);
        when(aiClientSelector.getClient()).thenReturn(aiClient);
        lenient().when(aiConfig.isApiKeyConfigured()).thenReturn(false);
        suggestionService = new OptimizationSuggestionService(
                jobMatchMapper, jobDescriptionMapper, resumeRepository,
                evidenceRepository, suggestionMapper, aiInvocationMapper,
                aiInvocationService, aiClientSelector, outputValidator,
                promptBuilder, objectMapper, companyProfileService, jdbcTemplate,
                matchingPipelineService, resumeAssessmentService, aiConfig
        );
    }

    // ── 入参校验 ──

    @Nested
    @DisplayName("入参校验")
    class InputValidation {

        @Test
        @DisplayName("generateSuggestions: jobMatchId 为 0 抛出异常")
        void shouldRejectZeroJobMatchId() {
            assertThatThrownBy(() -> suggestionService.generateSuggestions(0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("jobMatchId")
                    .hasMessageContaining("正整");
        }

        @Test
        @DisplayName("generateSuggestions: jobMatchId 为负数抛出异常")
        void shouldRejectNegativeJobMatchId() {
            assertThatThrownBy(() -> suggestionService.generateSuggestions(-1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("jobMatchId");
        }

        @Test
        @DisplayName("getSuggestions: jobMatchId 为 0 抛出异常")
        void shouldRejectZeroJobMatchIdOnGet() {
            assertThatThrownBy(() -> suggestionService.getSuggestions(0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("jobMatchId");
        }

        @Test
        @DisplayName("acceptSuggestion: suggestionId 为 0 抛出异常")
        void shouldRejectZeroSuggestionIdOnAccept() {
            assertThatThrownBy(() -> suggestionService.acceptSuggestion(0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("suggestionId");
        }

        @Test
        @DisplayName("rejectSuggestion: suggestionId 为 0 抛出异常")
        void shouldRejectZeroSuggestionIdOnReject() {
            assertThatThrownBy(() -> suggestionService.rejectSuggestion(0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("suggestionId");
        }
    }

    // ── getSuggestions ──

    @Nested
    @DisplayName("getSuggestions")
    class GetSuggestions {

        @Test
        @DisplayName("jobMatchId 为正数不抛出异常，返回空列表")
        void shouldReturnEmptyForValidJobMatchId() {
            when(suggestionMapper.selectList(any(QueryWrapper.class)))
                    .thenReturn(Collections.emptyList());

            GenerateSuggestionsResponse result = suggestionService.getSuggestions(1L);

            assertThat(result).isNotNull();
            assertThat(result.suggestions()).isEmpty();
        }

        @Test
        @DisplayName("存在建议时正确返回 DTO 列表")
        void shouldReturnDtosWhenSuggestionsExist() {
            OptimizationSuggestion entity = new OptimizationSuggestion();
            entity.setId(1L);
            entity.setJobMatchId(1L);
            entity.setResumeVersionId(10L);
            entity.setSectionKey("projects[0]");
            entity.setOriginalText("原文");
            entity.setSuggestedText("建议文本");
            entity.setReasonText("原因");
            entity.setTargetRequirement("JD要求");
            entity.setStatus("pending");
            entity.setCreatedAt(LocalDateTime.now());

            when(suggestionMapper.selectList(any(QueryWrapper.class)))
                    .thenReturn(List.of(entity));

            GenerateSuggestionsResponse result = suggestionService.getSuggestions(1L);

            assertThat(result.suggestions()).hasSize(1);
            OptimizationSuggestionDTO dto = result.suggestions().get(0);
            assertThat(dto.id()).isEqualTo(1L);
            assertThat(dto.sectionKey()).isEqualTo("projects[0]");
            assertThat(dto.status()).isEqualTo("pending");
        }
    }

    // ── generateSuggestions ──

    @Nested
    @DisplayName("generateSuggestions")
    class GenerateSuggestions {

        @Test
        @DisplayName("匹配结果不存在时抛出异常")
        void shouldThrowWhenJobMatchNotFound() {
            when(jobMatchMapper.selectById(1L)).thenReturn(null);

            assertThatThrownBy(() -> suggestionService.generateSuggestions(1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("匹配结果不存在");
        }

        @Test
        @DisplayName("简历版本不存在时抛出异常")
        void shouldThrowWhenResumeVersionNotFound() {
            JobMatch jobMatch = buildJobMatch();
            when(jobMatchMapper.selectById(1L)).thenReturn(jobMatch);
            when(resumeRepository.findVersionById(10L)).thenReturn(null);

            assertThatThrownBy(() -> suggestionService.generateSuggestions(1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("简历版本不存在");
        }

        @Test
        @DisplayName("岗位 JD 不存在时抛出异常")
        void shouldThrowWhenJobDescriptionNotFound() {
            JobMatch jobMatch = buildJobMatch();
            when(jobMatchMapper.selectById(1L)).thenReturn(jobMatch);
            when(resumeRepository.findVersionById(10L))
                    .thenReturn(new ResumeVersionDTO(10L, 100L, null, 1, Map.of("name", "test"), null, null, LocalDateTime.now()));
            when(jobDescriptionMapper.selectById(20L)).thenReturn(null);

            assertThatThrownBy(() -> suggestionService.generateSuggestions(1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("岗位 JD 不存在");
        }

        @Test
        @DisplayName("JD 未解析时抛出异常")
        void shouldThrowWhenJdNotParsed() {
            JobMatch jobMatch = buildJobMatch();
            JobDescription jd = new JobDescription();
            jd.setId(20L);
            jd.setParsedJson(null);

            when(jobMatchMapper.selectById(1L)).thenReturn(jobMatch);
            when(resumeRepository.findVersionById(10L))
                    .thenReturn(new ResumeVersionDTO(10L, 100L, null, 1, Map.of("name", "test"), null, null, LocalDateTime.now()));
            when(jobDescriptionMapper.selectById(20L)).thenReturn(jd);

            assertThatThrownBy(() -> suggestionService.generateSuggestions(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("尚未解析完成");
        }

        @Test
        @DisplayName("简历版本内容为空时抛出异常")
        void shouldThrowWhenResumeContentIsNull() {
            JobMatch jobMatch = buildJobMatch();
            JobDescription jd = buildJobDescription();

            when(jobMatchMapper.selectById(1L)).thenReturn(jobMatch);
            when(resumeRepository.findVersionById(10L))
                    .thenReturn(new ResumeVersionDTO(10L, 100L, null, 1, null, null, null, LocalDateTime.now()));
            when(jobDescriptionMapper.selectById(20L)).thenReturn(jd);

            assertThatThrownBy(() -> suggestionService.generateSuggestions(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("简历版本内容为空");
        }

        @Test
        @DisplayName("AI 调用异常时记录审计并返回空建议，不写入建议表")
        void shouldReturnEmptyWithoutPersistingWhenAiInvocationFails() {
            stubReadyToGenerate();
            when(aiClient.invoke(any())).thenThrow(new RuntimeException("provider unavailable"));

            GenerateSuggestionsResponse result = suggestionService.generateSuggestions(1L);

            assertThat(result.suggestions()).isEmpty();
            verify(aiInvocationService).logInvocationWithSchema(any(), any(), eq(false));
            verify(suggestionMapper, never()).insert(any(OptimizationSuggestion.class));
        }

        @Test
        @DisplayName("结构化 AI 输出应分别保存有证据建议和待补证据建议")
        void shouldPersistValidatedSuggestionsWithEvidenceStates() {
            stubReadyToGenerate();
            when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(10L))).thenReturn(List.of(7L));
            CapabilityEvidenceResponse evidence = new CapabilityEvidenceResponse(
                    7L, 1L, "project", "简历项目", "课程项目",
                    "Implemented Java service handling", "Completed regression tests",
                    List.of("Java", "Spring"), null, LocalDateTime.now(), LocalDateTime.now());
            when(evidenceRepository.findActiveByIdAndUserId(7L, 1L)).thenReturn(Optional.of(evidence));
            when(aiClient.invoke(any())).thenReturn(com.resumego.ai.AiResult.success("ai-1", """
                    {"suggestions":[
                      {"sectionKey":"summary","originalText":"已有项目经历完成后端模块开发","reason":"突出项目动作","targetRequirement":"Java","confidence":"high"},
                      {"sectionKey":"projects[0].description","originalText":"Implemented Java service handling","suggestedText":"Implemented Java service handling","reason":"关联项目证据","targetRequirement":"Spring","confidence":"medium","evidenceId":7}
                    ]}
                    """, 1, 1, 10));

            GenerateSuggestionsResponse response = suggestionService.generateSuggestions(1L);

            assertThat(response.suggestions()).hasSize(2);
            ArgumentCaptor<OptimizationSuggestion> captor = ArgumentCaptor.forClass(OptimizationSuggestion.class);
            verify(suggestionMapper, org.mockito.Mockito.times(2)).insert(captor.capture());
            assertThat(captor.getAllValues()).extracting(OptimizationSuggestion::getStatus)
                    .containsExactly("evidence_required", "pending");
            assertThat(captor.getAllValues().getFirst().getSuggestedText()).isNull();
            verify(aiInvocationService).logInvocationWithSchema(any(), any(), eq(true));
        }

        @Test
        @DisplayName("疑似新增重大事实的有证据建议应保留为高风险而非自动采纳")
        void shouldPersistFabricationAsHighRisk() {
            stubReadyToGenerate();
            when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(10L))).thenReturn(List.of(8L));
            CapabilityEvidenceResponse evidence = new CapabilityEvidenceResponse(
                    8L, 1L, "project", "课程项目", null,
                    "Implemented Java service", "Completed test", List.of("Java"), null,
                    LocalDateTime.now(), LocalDateTime.now());
            when(evidenceRepository.findActiveByIdAndUserId(8L, 1L)).thenReturn(Optional.of(evidence));
            when(aiClient.invoke(any())).thenReturn(com.resumego.ai.AiResult.success("ai-risk", """
                    {"suggestions":[{"sectionKey":"summary","originalText":"Implemented Java service","suggestedText":"Won national award and served 1000 users","reason":"展示成果","targetRequirement":"Java","confidence":"high","evidenceId":8}]}
                    """, 1, 1, 10));

            GenerateSuggestionsResponse response = suggestionService.generateSuggestions(1L);

            assertThat(response.suggestions()).hasSize(1);
            ArgumentCaptor<OptimizationSuggestion> captor = ArgumentCaptor.forClass(OptimizationSuggestion.class);
            verify(suggestionMapper).insert(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo("high_risk");
            assertThat(captor.getValue().getRiskLevel()).isEqualTo("high");
        }
    }

    // ── acceptSuggestion / rejectSuggestion ──

    @Nested
    @DisplayName("acceptSuggestion / rejectSuggestion")
    class AcceptReject {

        @Test
        @DisplayName("acceptSuggestion: 建议不存在抛出异常")
        void shouldThrowWhenAcceptingNonExistentSuggestion() {
            when(suggestionMapper.selectById(99999L)).thenReturn(null);

            assertThatThrownBy(() -> suggestionService.acceptSuggestion(99999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("建议不存在");
        }

        @Test
        @DisplayName("rejectSuggestion: 建议不存在抛出异常")
        void shouldThrowWhenRejectingNonExistentSuggestion() {
            when(suggestionMapper.selectById(99999L)).thenReturn(null);

            assertThatThrownBy(() -> suggestionService.rejectSuggestion(99999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("建议不存在");
        }

        @Test
        @DisplayName("acceptSuggestion: 非 pending 状态抛出异常")
        void shouldThrowWhenAcceptingNonPendingSuggestion() {
            OptimizationSuggestion suggestion = new OptimizationSuggestion();
            suggestion.setId(1L);
            suggestion.setStatus("accepted");
            when(suggestionMapper.selectById(1L)).thenReturn(suggestion);

            assertThatThrownBy(() -> suggestionService.acceptSuggestion(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("不允许采纳");
        }

        @Test
        @DisplayName("rejectSuggestion: 非 pending 状态抛出异常")
        void shouldThrowWhenRejectingNonPendingSuggestion() {
            OptimizationSuggestion suggestion = new OptimizationSuggestion();
            suggestion.setId(1L);
            suggestion.setStatus("rejected");
            when(suggestionMapper.selectById(1L)).thenReturn(suggestion);

            assertThatThrownBy(() -> suggestionService.rejectSuggestion(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("不允许拒绝");
        }

        @Test
        @DisplayName("acceptSuggestion: 无证据建议不可采纳")
        void shouldThrowWhenAcceptingSuggestionWithoutEvidence() {
            OptimizationSuggestion suggestion = new OptimizationSuggestion();
            suggestion.setId(1L);
            suggestion.setStatus("pending");
            suggestion.setSuggestedText(null);
            when(suggestionMapper.selectById(1L)).thenReturn(suggestion);

            assertThatThrownBy(() -> suggestionService.acceptSuggestion(1L))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("不可采纳");
        }

        @Test
        @DisplayName("acceptSuggestion: 正常采纳 pending 建议")
        void shouldAcceptPendingSuggestion() {
            OptimizationSuggestion suggestion = new OptimizationSuggestion();
            suggestion.setId(1L);
            suggestion.setResumeVersionId(10L);
            suggestion.setSectionKey("projects[0]");
            suggestion.setReasonText("JD 要求");
            suggestion.setStatus("pending");
            suggestion.setSuggestedText("建议文本");
            when(suggestionMapper.selectById(1L)).thenReturn(suggestion);
            when(suggestionMapper.updateById(any(OptimizationSuggestion.class))).thenReturn(1);

            ResumeVersionDTO currentVersion = new ResumeVersionDTO(
                    10L, 100L, null, 1, Map.of("projects", List.of("旧文本")),
                    null, "user", LocalDateTime.now());
            when(resumeRepository.findVersionById(10L)).thenReturn(currentVersion);
            when(resumeRepository.applySuggestion(anyString(), anyString(), anyString()))
                    .thenReturn("{\"projects\":[\"建议文本\"]}");
            when(resumeRepository.findMaxVersionNo(100L)).thenReturn(1);
            when(resumeRepository.insertVersion(
                    anyLong(), anyLong(), anyInt(), anyString(), anyString(), anyString()))
                    .thenReturn(11L);

            assertDoesNotThrow(() -> suggestionService.acceptSuggestion(1L));

            ArgumentCaptor<OptimizationSuggestion> captor =
                    ArgumentCaptor.forClass(OptimizationSuggestion.class);
            verify(suggestionMapper).updateById(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo("accepted");
            assertThat(captor.getValue().getAcceptedVersionId()).isEqualTo(11L);
            assertThat(captor.getValue().getDecidedAt()).isNotNull();
        }

        @Test
        @DisplayName("rejectSuggestion: 正常拒绝 pending 建议")
        void shouldRejectPendingSuggestion() {
            OptimizationSuggestion suggestion = new OptimizationSuggestion();
            suggestion.setId(1L);
            suggestion.setStatus("pending");
            when(suggestionMapper.selectById(1L)).thenReturn(suggestion);
            when(suggestionMapper.updateById(any(OptimizationSuggestion.class))).thenReturn(1);

            assertDoesNotThrow(() -> suggestionService.rejectSuggestion(1L));

            ArgumentCaptor<OptimizationSuggestion> captor =
                    ArgumentCaptor.forClass(OptimizationSuggestion.class);
            verify(suggestionMapper).updateById(captor.capture());
            assertThat(captor.getValue().getStatus()).isEqualTo("rejected");
            assertThat(captor.getValue().getDecidedAt()).isNotNull();
        }
    }

    // ── 辅助方法 ──

    private JobMatch buildJobMatch() {
        JobMatch jobMatch = new JobMatch();
        jobMatch.setId(1L);
        jobMatch.setResumeVersionId(10L);
        jobMatch.setJobDescriptionId(20L);
        jobMatch.setDetailsJson("{\"gaps\":[]}");
        return jobMatch;
    }

    private JobDescription buildJobDescription() {
        JobDescription jd = new JobDescription();
        jd.setId(20L);
        jd.setParsedJson("{\"title\":\"后端开发\"}");
        return jd;
    }

    private void stubReadyToGenerate() {
        when(jobMatchMapper.selectById(1L)).thenReturn(buildJobMatch());
        when(resumeRepository.findVersionById(10L)).thenReturn(new ResumeVersionDTO(
                10L, 100L, null, 1,
                Map.of("summary", "基于已有项目经历完成后端模块开发与测试回归。"),
                null, "user", LocalDateTime.now()
        ));
        when(jobDescriptionMapper.selectById(20L)).thenReturn(buildJobDescription());
        lenient().when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(10L))).thenReturn(List.of());
        AiInvocation invocation = new AiInvocation();
        invocation.setId(99L);
        when(aiInvocationMapper.selectOne(any(QueryWrapper.class))).thenReturn(invocation);
    }
}
