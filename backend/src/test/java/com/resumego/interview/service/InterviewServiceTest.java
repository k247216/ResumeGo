package com.resumego.interview.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumego.ai.AiClient;
import com.resumego.ai.AiClientSelector;
import com.resumego.ai.AiErrorCategory;
import com.resumego.ai.AiInvocationMapper;
import com.resumego.ai.AiInvocationService;
import com.resumego.ai.AiRequest;
import com.resumego.ai.AiResult;
import com.resumego.ai.validate.AiOutputValidator;
import com.resumego.company.CompanyProfileService;
import com.resumego.interview.InterviewState;
import com.resumego.interview.dto.InterviewQuestionDTO;
import com.resumego.interview.dto.InterviewStatusResponse;
import com.resumego.interview.dto.MultiSessionSummaryRequest;
import com.resumego.interview.dto.MultiSessionSummaryResponse;
import com.resumego.interview.dto.SessionHistoryResponse;
import com.resumego.interview.dto.StartInterviewRequest;
import com.resumego.interview.dto.SubmitAnswerRequest;
import com.resumego.interview.dto.SubmitAnswerResponse;
import com.resumego.interview.entity.InterviewAnswer;
import com.resumego.interview.entity.InterviewEvaluation;
import com.resumego.interview.entity.InterviewPlan;
import com.resumego.interview.entity.InterviewQuestion;
import com.resumego.interview.entity.InterviewSession;
import com.resumego.interview.entity.InterviewerPersona;
import com.resumego.interview.mapper.InterviewAnswerMapper;
import com.resumego.interview.mapper.InterviewEvaluationMapper;
import com.resumego.interview.mapper.InterviewPlanMapper;
import com.resumego.interview.mapper.InterviewQuestionMapper;
import com.resumego.interview.mapper.InterviewSessionMapper;
import com.resumego.interview.mapper.InterviewerPersonaMapper;
import com.resumego.job.JobDescription;
import com.resumego.job.JobDescriptionMapper;
import com.resumego.resume.dto.ResumeVersionDTO;
import com.resumego.resume.repository.ResumeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * InterviewService 单元测试。
 * 使用 Mockito 模拟所有外部依赖，不依赖 Spring 上下文。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InterviewService 单元测试")
class InterviewServiceTest {

    @Mock
    private InterviewSessionMapper sessionMapper;
    @Mock
    private InterviewQuestionMapper questionMapper;
    @Mock
    private InterviewAnswerMapper answerMapper;
    @Mock
    private InterviewEvaluationMapper evaluationMapper;
    @Mock
    private InterviewPlanMapper planMapper;
    @Mock
    private AiClient aiClient;
    @Mock
    private AiClientSelector aiClientSelector;
    @Mock
    private AiInvocationService aiInvocationService;
    @Mock
    private AiInvocationMapper aiInvocationMapper;
    @Mock
    private InterviewerPersonaMapper personaMapper;

    @Mock
    private ResumeRepository resumeRepository;
    @Mock
    private JobDescriptionMapper jobDescriptionMapper;
    @Mock
    private CompanyProfileService companyProfileService;

    private InterviewStateMachine stateMachine;
    private InterviewPromptBuilder promptBuilder;
    private AiOutputValidator outputValidator;
    private ObjectMapper objectMapper;
    private InterviewService interviewService;

    private static final String QUESTION_JSON = """
            {
              "questionText": "请描述你最有挑战的项目",
              "questionType": "technical",
              "targetSkill": "问题解决能力"
            }
            """;

    private static final String EVALUATION_JSON = """
            {
              "score": {"clarity": 8, "relevance": 7, "depth": 6, "accuracy": 8},
              "strengths": ["表达清晰"],
              "weaknesses": ["缺少量化数据"],
              "suggestions": ["建议补充具体指标"],
              "referenceAnswer": "参考回答示例"
            }
            """;

    private static final String EVALUATION_DECIMAL_JSON = """
            {
              "score": {"clarity": 8.6, "relevance": "7.4", "depth": 6.2, "accuracy": "8"},
              "strengths": ["表达清晰"],
              "weaknesses": ["缺少量化数据"],
              "suggestions": ["建议补充具体指标"],
              "referenceAnswer": "参考回答示例"
            }
            """;

    private static final String EVALUATION_PERCENT_JSON = """
            {
              "score": {"clarity": "86/100", "relevance": 74, "depth": "62分", "accuracy": 80},
              "strengths": ["表达清晰"],
              "weaknesses": ["缺少量化数据"],
              "suggestions": ["建议补充具体指标"]
            }
            """;

    private static final String EVALUATION_WITHOUT_REFERENCE_JSON = """
            {
              "score": {"clarity": 8, "relevance": 7, "depth": 6, "accuracy": 8},
              "strengths": ["回答结构清楚"],
              "weaknesses": ["缺少量化结果"],
              "suggestions": ["补充结果证据"]
            }
            """;

    private static final String EVALUATION_FIVE_DIMENSIONS_JSON = """
            {
              "score": {"clarity": 8, "relevance": 7, "depth": 6, "structure": 8, "evidence": 7},
              "strengths": ["表达清晰"],
              "weaknesses": ["缺少量化数据"],
              "suggestions": ["补充技术取舍"],
              "referenceAnswer": ""
            }
            """;

    private static final String SUMMARY_JSON = """
            {
              "overallScore": 75,
              "dimensionScores": {"technical": 8, "communication": 7, "problemSolving": 6},
              "strengths": ["技术基础扎实"],
              "weaknesses": ["表达可以更精炼"],
              "suggestions": ["多练习行为面试题"]
            }
            """;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        stateMachine = new InterviewStateMachine();
        promptBuilder = new InterviewPromptBuilder(objectMapper);
        outputValidator = new AiOutputValidator(objectMapper);
        when(aiClientSelector.getClient()).thenReturn(aiClient);
        lenient().when(aiClientSelector.isConfigured()).thenReturn(true);

        interviewService = new InterviewService(
                sessionMapper, questionMapper, answerMapper, evaluationMapper,
                planMapper, stateMachine, promptBuilder, personaMapper, aiClientSelector,
                aiInvocationService, aiInvocationMapper, outputValidator,
                resumeRepository, jobDescriptionMapper, companyProfileService, objectMapper
        );
    }

    // ── createInterview ──

    @Nested
    @DisplayName("createInterview")
    class CreateInterview {

        @Test
        @DisplayName("简历和岗位存在时创建成功")
        void shouldCreateInterviewSuccessfully() {
            when(resumeRepository.findVersionById(10L))
                    .thenReturn(new ResumeVersionDTO(10L, 100L, null, 1, Map.of("name", "test"),
                            null, null, LocalDateTime.now()));
            when(jobDescriptionMapper.selectById(20L)).thenReturn(buildJd());
            when(personaMapper.selectById(1L)).thenReturn(buildPersona());

            InterviewStatusResponse response = interviewService.createInterview(
                    new StartInterviewRequest(10L, 20L, 5, 1L));

            assertThat(response).isNotNull();
            assertThat(response.status()).isEqualTo(InterviewState.READY.name());
            assertThat(response.currentQuestionIndex()).isEqualTo(0);
            assertThat(response.totalQuestions()).isEqualTo(5);
            assertThat(response.currentQuestion()).isNull();
            assertThat(response.completed()).isFalse();

            ArgumentCaptor<InterviewSession> captor = ArgumentCaptor.forClass(InterviewSession.class);
            verify(sessionMapper).insert(captor.capture());
            InterviewSession saved = captor.getValue();
            assertThat(saved.getResumeVersionId()).isEqualTo(10L);
            assertThat(saved.getJobDescriptionId()).isEqualTo(20L);
            assertThat(saved.getStatus()).isEqualTo(InterviewState.READY.name());
        }

        @Test
        @DisplayName("简历版本不存在时抛出异常")
        void shouldThrowWhenResumeVersionNotFound() {
            when(resumeRepository.findVersionById(10L)).thenReturn(null);

            assertThatThrownBy(() -> interviewService.createInterview(
                    new StartInterviewRequest(10L, 20L, 5, 1L)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("简历版本不存在");
        }

        @Test
        @DisplayName("岗位不存在时抛出异常")
        void shouldThrowWhenJdNotFound() {
            when(resumeRepository.findVersionById(10L))
                    .thenReturn(new ResumeVersionDTO(10L, 100L, null, 1, Map.of("name", "test"),
                            null, null, LocalDateTime.now()));
            when(jobDescriptionMapper.selectById(20L)).thenReturn(null);

            assertThatThrownBy(() -> interviewService.createInterview(
                    new StartInterviewRequest(10L, 20L, 5, 1L)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("岗位不存在");
        }
    }

    @Nested
    @DisplayName("listMyInterviews")
    class ListMyInterviews {

        @Test
        @DisplayName("已隐藏或非当前用户面试计划下的会话不应回流为历史记录")
        void shouldFilterSessionsBelongingToHiddenPlans() {
            InterviewSession legacy = buildSession(1L, InterviewState.COMPLETED, 3);
            InterviewSession activePlanSession = buildSession(2L, InterviewState.COMPLETED, 3);
            activePlanSession.setPlanId(100L);
            InterviewSession hiddenPlanSession = buildSession(3L, InterviewState.COMPLETED, 3);
            hiddenPlanSession.setPlanId(200L);
            InterviewSession foreignPlanSession = buildSession(4L, InterviewState.COMPLETED, 3);
            foreignPlanSession.setPlanId(300L);
            when(sessionMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(
                    legacy,
                    activePlanSession,
                    hiddenPlanSession,
                    foreignPlanSession
            ));
            InterviewPlan activePlan = buildPlan(100L, null);
            InterviewPlan hiddenPlan = buildPlan(200L, LocalDateTime.now());
            InterviewPlan foreignPlan = buildPlan(300L, null);
            foreignPlan.setUserId(99L);
            when(planMapper.selectBatchIds(List.of(100L, 200L, 300L))).thenReturn(List.of(
                    activePlan,
                    hiddenPlan,
                    foreignPlan
            ));

            List<InterviewStatusResponse> response = interviewService.listMyInterviews();

            assertThat(response).extracting(InterviewStatusResponse::sessionId)
                    .containsExactly(1L, 2L);
        }
    }

    // ── getInterviewStatus ──

    @Nested
    @DisplayName("getInterviewStatus")
    class GetInterviewStatus {

        @Test
        @DisplayName("会话存在时返回状态")
        void shouldReturnStatusWhenSessionExists() {
            InterviewSession session = buildSession(1L, InterviewState.READY, 0);
            when(sessionMapper.selectById(1L)).thenReturn(session);

            InterviewStatusResponse response = interviewService.getInterviewStatus(1L);

            assertThat(response).isNotNull();
            assertThat(response.sessionId()).isEqualTo(1L);
            assertThat(response.status()).isEqualTo(InterviewState.READY.name());
            assertThat(response.currentQuestion()).isNull();
        }

        @Test
        @DisplayName("当前有题目时返回题目信息")
        void shouldReturnCurrentQuestion() {
            InterviewSession session = buildSession(1L, InterviewState.WAITING_ANSWER, 1);
            when(sessionMapper.selectById(1L)).thenReturn(session);
            when(questionMapper.selectOne(any(QueryWrapper.class)))
                    .thenReturn(buildQuestion(1L, 1, "描述你的项目", "technical"));

            InterviewStatusResponse response = interviewService.getInterviewStatus(1L);

            assertThat(response.currentQuestion()).isNotNull();
            assertThat(response.currentQuestion().questionText()).isEqualTo("描述你的项目");
        }

        @Test
        @DisplayName("会话不存在时抛出异常")
        void shouldThrowWhenSessionNotFound() {
            when(sessionMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> interviewService.getInterviewStatus(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("面试会话不存在");
        }
    }

    // ── startInterview ──

    @Nested
    @DisplayName("startInterview")
    class StartInterview {

        @Test
        @DisplayName("正常开始面试，生成第一题并返回 WAITING_ANSWER")
        void shouldStartInterviewAndGenerateQuestion() {
            InterviewSession session = buildSession(1L, InterviewState.READY, 0);
            when(sessionMapper.selectById(1L)).thenReturn(session);
            when(resumeRepository.findVersionById(10L))
                    .thenReturn(new ResumeVersionDTO(10L, 100L, null, 1, Map.of("name", "test"),
                            null, null, LocalDateTime.now()));
            when(jobDescriptionMapper.selectById(20L)).thenReturn(buildJd());

            AiResult successResult = AiResult.success("req-1", QUESTION_JSON, 100, 50, 500);
            when(aiClient.invoke(any())).thenReturn(successResult);

            InterviewStatusResponse response = interviewService.startInterview(1L);

            assertThat(response).isNotNull();
            assertThat(response.status()).isEqualTo(InterviewState.WAITING_ANSWER.name());
            assertThat(response.currentQuestionIndex()).isEqualTo(1);
            assertThat(response.currentQuestion()).isNotNull();
            assertThat(response.currentQuestion().questionText()).isEqualTo("请描述你最有挑战的项目");
            assertThat(response.currentQuestion().questionType()).isEqualTo("technical");
            assertThat(response.completed()).isFalse();

            ArgumentCaptor<InterviewQuestion> questionCaptor = ArgumentCaptor.forClass(InterviewQuestion.class);
            verify(questionMapper).insert(questionCaptor.capture());
            assertThat(questionCaptor.getValue().getSource()).isEqualTo("ai_generated");
            verify(sessionMapper, atLeastOnce()).updateById(any(InterviewSession.class));
        }

        @Test
        @DisplayName("AI 失败时标记 FAILED 并返回")
        void shouldMarkFailedWhenAiFails() {
            InterviewSession session = buildSession(1L, InterviewState.READY, 0);
            when(sessionMapper.selectById(1L)).thenReturn(session);
            when(resumeRepository.findVersionById(10L))
                    .thenReturn(new ResumeVersionDTO(10L, 100L, null, 1, Map.of("name", "test"),
                            null, null, LocalDateTime.now()));
            when(jobDescriptionMapper.selectById(20L)).thenReturn(buildJd());

            AiResult failResult = AiResult.failure("req-1", AiErrorCategory.PROVIDER_ERROR,
                    "服务异常", 500);
            when(aiClient.invoke(any())).thenReturn(failResult);

            InterviewStatusResponse response = interviewService.startInterview(1L);

            assertThat(response.status()).isEqualTo(InterviewState.FAILED.name());
            assertThat(response.completed()).isTrue();
            assertThat(response.currentQuestion()).isNull();
        }

        @Test
        @DisplayName("会话不存在时抛出异常")
        void shouldThrowWhenSessionNotFound() {
            when(sessionMapper.selectById(999L)).thenReturn(null);

            assertThatThrownBy(() -> interviewService.startInterview(999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("面试会话不存在");
        }
    }

    // ── submitAnswer ──

    @Nested
    @DisplayName("submitAnswer")
    class SubmitAnswer {

        @Test
        @DisplayName("非最后一题：提交回答后生成下一题")
        void shouldGenerateNextQuestion() {
            // 第 1 题提交
            InterviewSession session = buildSession(1L, InterviewState.WAITING_ANSWER, 1);
            when(sessionMapper.selectById(1L)).thenReturn(session);
            when(questionMapper.selectOne(any(QueryWrapper.class)))
                    .thenReturn(buildQuestion(10L, 1, "问题1", "technical"));
            when(jobDescriptionMapper.selectById(20L)).thenReturn(buildJd());
            when(resumeRepository.findVersionById(10L))
                    .thenReturn(new ResumeVersionDTO(10L, 100L, null, 1, Map.of("name", "test"),
                            null, null, LocalDateTime.now()));

            // AI 评价成功
            AiResult evalResult = AiResult.success("req-eval", EVALUATION_JSON, 100, 50, 500);
            // AI 生成下一题成功
            AiResult questionResult = AiResult.success("req-q2", QUESTION_JSON, 100, 50, 500);
            when(aiClient.invoke(any())).thenReturn(evalResult, questionResult);

            SubmitAnswerResponse response = interviewService.submitAnswer(1L,
                    new SubmitAnswerRequest("我的回答"));

            assertThat(response).isNotNull();
            assertThat(response.status()).isEqualTo(InterviewState.WAITING_ANSWER.name());
            assertThat(response.currentQuestionIndex()).isEqualTo(2);
            assertThat(response.nextQuestion()).isNotNull();
            assertThat(response.nextQuestion().questionText()).isEqualTo("请描述你最有挑战的项目");
            assertThat(response.evaluation()).isNotNull();
            assertThat(response.completed()).isFalse();

            verify(answerMapper).insert(any(InterviewAnswer.class));
            verify(evaluationMapper).insert(any(InterviewEvaluation.class));
            verify(questionMapper).insert(any(InterviewQuestion.class));
        }

        @Test
        @DisplayName("最后一题：提交后生成总结并完成")
        void shouldGenerateSummaryAndComplete() {
            InterviewSession session = buildSession(1L, InterviewState.WAITING_ANSWER, 3);
            when(sessionMapper.selectById(1L)).thenReturn(session);
            when(questionMapper.selectOne(any(QueryWrapper.class)))
                    .thenReturn(buildQuestion(30L, 3, "问题3", "technical"));
            when(jobDescriptionMapper.selectById(20L)).thenReturn(buildJd());

            // AI 评价成功
            AiResult evalResult = AiResult.success("req-eval", EVALUATION_JSON, 100, 50, 500);
            // AI 总结成功
            AiResult summaryResult = AiResult.success("req-summary", SUMMARY_JSON, 100, 50, 500);
            when(aiClient.invoke(any())).thenReturn(evalResult, summaryResult);

            // 总结需要查询所有问题和回答
            when(questionMapper.selectList(any(QueryWrapper.class)))
                    .thenReturn(java.util.List.of(
                            buildQuestion(10L, 1, "问题1", "technical"),
                            buildQuestion(20L, 2, "问题2", "behavioral"),
                            buildQuestion(30L, 3, "问题3", "technical")
                    ));
            when(answerMapper.selectOne(any(QueryWrapper.class)))
                    .thenReturn(buildAnswer(1L, 10L, "回答1"));

            SubmitAnswerResponse response = interviewService.submitAnswer(1L,
                    new SubmitAnswerRequest("我的回答"));

            assertThat(response).isNotNull();
            assertThat(response.status()).isEqualTo(InterviewState.COMPLETED.name());
            assertThat(response.nextQuestion()).isNull();
            assertThat(response.evaluation()).isNotNull();
            assertThat(response.completed()).isTrue();

            verify(evaluationMapper).insert(any(InterviewEvaluation.class));
            ArgumentCaptor<InterviewSession> sessionCaptor =
                    ArgumentCaptor.forClass(InterviewSession.class);
            verify(sessionMapper, atLeastOnce()).updateById(sessionCaptor.capture());
            // 最后一次 updateById 应包含 summaryJson，且 completedAt 已设置
            InterviewSession lastUpdate = sessionCaptor.getValue();
            assertThat(lastUpdate.getSummaryJson()).isNotNull();
            assertThat(lastUpdate.getCompletedAt()).isNotNull();
        }

        @Test
        @DisplayName("知识训练或真题演练未绑定岗位简历时仍应完成评价")
        void shouldEvaluateSourceSessionWithoutJobOrResume() {
            InterviewSession session = buildSession(1L, InterviewState.WAITING_ANSWER, 1);
            session.setTotalQuestions(1);
            session.setResumeVersionId(null);
            session.setJobDescriptionId(null);
            session.setPlanId(88L);
            when(sessionMapper.selectById(1L)).thenReturn(session);
            when(questionMapper.selectOne(any(QueryWrapper.class)))
                    .thenReturn(buildQuestion(10L, 1, "Redis 如何保证缓存一致性？", "knowledge"));
            when(questionMapper.selectList(any(QueryWrapper.class)))
                    .thenReturn(List.of(buildQuestion(10L, 1, "Redis 如何保证缓存一致性？", "knowledge")));
            when(answerMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);
            when(aiClient.invoke(any())).thenReturn(
                    AiResult.success("source-eval", EVALUATION_FIVE_DIMENSIONS_JSON, 100, 50, 250),
                    AiResult.success("source-summary", SUMMARY_JSON, 100, 50, 250));

            SubmitAnswerResponse response = interviewService.submitAnswer(1L,
                    new SubmitAnswerRequest("通过双写校验和失效策略保持一致性"));

            assertThat(response.completed()).isTrue();
            assertThat(response.retryable()).isFalse();
            assertThat(response.evaluation()).isNotNull();
            assertThat(response.evaluation().score().evidence()).isEqualTo(7);
            verify(evaluationMapper).insert(any(InterviewEvaluation.class));
        }

        @Test
        @DisplayName("模型返回小数或数字字符串时应归一化为可复盘的整数评分")
        void shouldNormalizeProviderScoreValues() {
            InterviewSession session = buildSession(1L, InterviewState.WAITING_ANSWER, 1);
            when(sessionMapper.selectById(1L)).thenReturn(session);
            when(questionMapper.selectOne(any(QueryWrapper.class)))
                    .thenReturn(buildQuestion(30L, 1, "问题1", "technical"));
            when(jobDescriptionMapper.selectById(20L)).thenReturn(buildJd());
            when(resumeRepository.findVersionById(10L))
                    .thenReturn(new ResumeVersionDTO(10L, 100L, null, 1, Map.of("name", "test"),
                            null, null, LocalDateTime.now()));
            when(aiClient.invoke(any())).thenReturn(
                    AiResult.success("req-eval-decimal", EVALUATION_DECIMAL_JSON, 100, 50, 500),
                    AiResult.success("req-q2", QUESTION_JSON, 100, 50, 500));

            SubmitAnswerResponse response = interviewService.submitAnswer(1L,
                    new SubmitAnswerRequest("我的回答包含真实的技术动作和结果"));

            assertThat(response.evaluation()).isNotNull();
            assertThat(response.evaluation().score().clarity()).isEqualTo(9);
            assertThat(response.evaluation().score().relevance()).isEqualTo(7);
            assertThat(response.evaluation().score().depth()).isEqualTo(6);
            assertThat(response.evaluation().score().accuracy()).isEqualTo(8);
        }

        @Test
        @DisplayName("兼容模型返回百分制或带单位的评分，仍应完成评价")
        void shouldNormalizePercentProviderScoreValues() {
            InterviewSession session = buildSession(1L, InterviewState.WAITING_ANSWER, 1);
            when(sessionMapper.selectById(1L)).thenReturn(session);
            when(questionMapper.selectOne(any(QueryWrapper.class)))
                    .thenReturn(buildQuestion(30L, 1, "问题1", "technical"));
            when(jobDescriptionMapper.selectById(20L)).thenReturn(buildJd());
            when(resumeRepository.findVersionById(10L))
                    .thenReturn(new ResumeVersionDTO(10L, 100L, null, 1, Map.of(), null, null, LocalDateTime.now()));
            when(aiClient.invoke(any())).thenReturn(
                    AiResult.success("req-eval-percent", EVALUATION_PERCENT_JSON, 100, 50, 500),
                    AiResult.success("req-q2", QUESTION_JSON, 100, 50, 500));

            SubmitAnswerResponse response = interviewService.submitAnswer(1L,
                    new SubmitAnswerRequest("基于真实项目补充了技术动作和结果"));

            assertThat(response.evaluation()).isNotNull();
            assertThat(response.evaluation().score().clarity()).isEqualTo(9);
            assertThat(response.evaluation().score().relevance()).isEqualTo(7);
            assertThat(response.evaluation().score().depth()).isEqualTo(6);
            assertThat(response.evaluation().score().accuracy()).isEqualTo(8);
        }

        @Test
        @DisplayName("评价应返回与主页一致的五个能力维度")
        void shouldExposeFiveHomepageDimensions() {
            InterviewSession session = buildSession(1L, InterviewState.WAITING_ANSWER, 1);
            when(sessionMapper.selectById(1L)).thenReturn(session);
            when(questionMapper.selectOne(any(QueryWrapper.class)))
                    .thenReturn(buildQuestion(30L, 1, "问题1", "technical"));
            when(jobDescriptionMapper.selectById(20L)).thenReturn(buildJd());
            when(resumeRepository.findVersionById(10L))
                    .thenReturn(new ResumeVersionDTO(10L, 100L, null, 1, Map.of(), null, null, LocalDateTime.now()));
            when(aiClient.invoke(any())).thenReturn(
                    AiResult.success("req-eval-five", EVALUATION_FIVE_DIMENSIONS_JSON, 100, 50, 500),
                    AiResult.success("req-q2", QUESTION_JSON, 100, 50, 500));

            SubmitAnswerResponse response = interviewService.submitAnswer(1L,
                    new SubmitAnswerRequest("基于真实项目补充了技术动作和结果"));

            assertThat(response.evaluation()).isNotNull();
            assertThat(response.evaluation().score().structure()).isEqualTo(8);
            assertThat(response.evaluation().score().evidence()).isEqualTo(7);
        }

        @Test
        @DisplayName("模型省略无关上下文字段时仍应保留评价并使用空参考回答")
        void shouldAcceptEvaluationWithoutReferenceAnswer() {
            InterviewSession session = buildSession(1L, InterviewState.WAITING_ANSWER, 1);
            when(sessionMapper.selectById(1L)).thenReturn(session);
            when(questionMapper.selectOne(any(QueryWrapper.class)))
                    .thenReturn(buildQuestion(30L, 1, "问题1", "technical"));
            when(jobDescriptionMapper.selectById(20L)).thenReturn(buildJd());
            when(resumeRepository.findVersionById(10L))
                    .thenReturn(new ResumeVersionDTO(10L, 100L, null, 1, Map.of(), null, null, LocalDateTime.now()));
            when(aiClient.invoke(any())).thenReturn(
                    AiResult.success("req-eval-no-reference", EVALUATION_WITHOUT_REFERENCE_JSON, 100, 50, 500),
                    AiResult.success("req-q2", QUESTION_JSON, 100, 50, 500));

            SubmitAnswerResponse response = interviewService.submitAnswer(1L,
                    new SubmitAnswerRequest("基于资料的回答"));

            assertThat(response.evaluation()).isNotNull();
            assertThat(response.evaluation().referenceAnswer()).isEmpty();
        }

        @Test
        @DisplayName("AI 评价失败时返回可重试标记")
        void shouldMarkFailedWhenEvaluationFails() {
            InterviewSession session = buildSession(1L, InterviewState.WAITING_ANSWER, 1);
            when(sessionMapper.selectById(1L)).thenReturn(session);
            when(questionMapper.selectOne(any(QueryWrapper.class)))
                    .thenReturn(buildQuestion(10L, 1, "问题1", "technical"));
            when(jobDescriptionMapper.selectById(20L)).thenReturn(buildJd());

            AiResult failResult = AiResult.failure("req-1", AiErrorCategory.PROVIDER_ERROR,
                    "服务异常", 500);
            when(aiClient.invoke(any())).thenReturn(failResult);

            SubmitAnswerResponse response = interviewService.submitAnswer(1L,
                    new SubmitAnswerRequest("我的回答"));

            assertThat(response.retryable()).isTrue();
            assertThat(response.status()).isEqualTo(InterviewState.WAITING_ANSWER.name());
            assertThat(response.completed()).isFalse();
            assertThat(response.evaluation()).isNull();
            assertThat(response.errorCode()).isEqualTo(AiErrorCategory.PROVIDER_ERROR.name());
            assertThat(response.errorMessage()).isEqualTo("服务异常");

            verify(answerMapper).insert(any(InterviewAnswer.class));
        }

        @Test
        @DisplayName("重复提交已经评价过的回答时应复用评价，避免唯一键内部错误")
        void shouldReuseExistingEvaluationOnDuplicateSubmit() {
            InterviewSession session = buildSession(1L, InterviewState.WAITING_ANSWER, 1);
            session.setTotalQuestions(1);
            InterviewQuestion question = buildQuestion(10L, 1, "问题1", "technical");
            InterviewAnswer answer = buildAnswer(20L, 10L, "不懂");
            InterviewEvaluation existingEvaluation = buildEvaluation(20L, 10L);

            when(sessionMapper.selectById(1L)).thenReturn(session);
            when(questionMapper.selectOne(any(QueryWrapper.class))).thenReturn(question);
            when(answerMapper.selectOne(any(QueryWrapper.class))).thenReturn(answer);
            when(evaluationMapper.selectOne(any(QueryWrapper.class))).thenReturn(existingEvaluation);
            when(aiClient.invoke(any())).thenReturn(
                    AiResult.success("req-summary", SUMMARY_JSON, 100, 50, 200));

            SubmitAnswerResponse response = interviewService.submitAnswer(1L,
                    new SubmitAnswerRequest("不懂"));

            assertThat(response.evaluation()).isNotNull();
            assertThat(response.evaluation().score().clarity()).isEqualTo(6);
            assertThat(response.retryable()).isFalse();
            ArgumentCaptor<AiRequest> aiRequestCaptor = ArgumentCaptor.forClass(AiRequest.class);
            verify(aiClient).invoke(aiRequestCaptor.capture());
            assertThat(aiRequestCaptor.getValue().featureType()).isEqualTo("interview_summary");
            verify(evaluationMapper, never()).insert(any(InterviewEvaluation.class));
        }

        @Test
        @DisplayName("状态不允许提交回答时抛出异常")
        void shouldThrowWhenStateNotAllowed() {
            InterviewSession session = buildSession(1L, InterviewState.READY, 0);
            when(sessionMapper.selectById(1L)).thenReturn(session);

            assertThatThrownBy(() -> interviewService.submitAnswer(1L,
                    new SubmitAnswerRequest("回答")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("当前状态不允许提交回答");
        }

        @Test
        @DisplayName("当前问题不存在时抛出异常")
        void shouldThrowWhenCurrentQuestionNotFound() {
            InterviewSession session = buildSession(1L, InterviewState.WAITING_ANSWER, 1);
            when(sessionMapper.selectById(1L)).thenReturn(session);
            when(questionMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);

            assertThatThrownBy(() -> interviewService.submitAnswer(1L,
                    new SubmitAnswerRequest("回答")))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("当前问题不存在");
        }

        @Test
        @DisplayName("AI 评价输出校验失败时返回可重试标记")
        void shouldMarkFailedWhenEvaluationJsonInvalid() {
            InterviewSession session = buildSession(1L, InterviewState.WAITING_ANSWER, 1);
            when(sessionMapper.selectById(1L)).thenReturn(session);
            when(questionMapper.selectOne(any(QueryWrapper.class)))
                    .thenReturn(buildQuestion(10L, 1, "问题1", "technical"));
            when(jobDescriptionMapper.selectById(20L)).thenReturn(buildJd());

            // 返回非法 JSON（缺少必填字段）
            AiResult badResult = AiResult.success("req-1", "{\"wrong\": \"format\"}", 100, 50, 500);
            when(aiClient.invoke(any())).thenReturn(badResult);

            SubmitAnswerResponse response = interviewService.submitAnswer(1L,
                    new SubmitAnswerRequest("我的回答"));

            assertThat(response.retryable()).isTrue();
            assertThat(response.status()).isEqualTo(InterviewState.WAITING_ANSWER.name());
            assertThat(response.completed()).isFalse();
        }

        @Test
        @DisplayName("空白回答应抛出异常")
        void shouldRejectBlankAnswer() {
            InterviewSession session = buildSession(1L, InterviewState.WAITING_ANSWER, 1);
            when(sessionMapper.selectById(1L)).thenReturn(session);

            assertThatThrownBy(() -> interviewService.submitAnswer(1L,
                    new SubmitAnswerRequest("   ")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("回答内容不能为空");
        }

        @Test
        @DisplayName("超长回答（>10K 字符）应抛出异常")
        void shouldRejectOversizedAnswer() {
            InterviewSession session = buildSession(1L, InterviewState.WAITING_ANSWER, 1);
            when(sessionMapper.selectById(1L)).thenReturn(session);

            StringBuilder sb = new StringBuilder(10_001);
            for (int i = 0; i < 10_001; i++) {
                sb.append('x');
            }

            assertThatThrownBy(() -> interviewService.submitAnswer(1L,
                    new SubmitAnswerRequest(sb.toString())))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("回答内容过长");
        }
    }

    // ── 历史与 AI 总结（不覆盖、不决定状态机转换） ──

    @Nested
    @DisplayName("getSessionHistory")
    class SessionHistory {

        @Test
        @DisplayName("应组装问答、结构化评价与逐维分数")
        void shouldBuildHistoryWithEvaluation() {
            InterviewSession session = buildSession(1L, InterviewState.COMPLETED, 3);
            InterviewQuestion question = buildQuestion(10L, 1, "如何设计限流？", "technical");
            InterviewAnswer answer = buildAnswer(20L, 10L, "使用令牌桶");
            InterviewEvaluation evaluation = new InterviewEvaluation();
            evaluation.setQuestionId(10L);
            evaluation.setScoreJson("{\"clarity\":8,\"relevance\":7,\"depth\":6,\"accuracy\":9}");
            evaluation.setStrengthsJson("[\"表达清楚\"]");
            evaluation.setWeaknessesJson("[\"缺少指标\"]");
            evaluation.setSuggestionsJson("[\"补充压测数据\"]");
            evaluation.setReferenceAnswerJson("基于项目的真实限流方案");
            when(sessionMapper.selectById(1L)).thenReturn(session);
            when(questionMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of(question));
            when(answerMapper.selectOne(any(QueryWrapper.class))).thenReturn(answer);
            when(evaluationMapper.selectOne(any(QueryWrapper.class))).thenReturn(evaluation);

            SessionHistoryResponse response = interviewService.getSessionHistory(1L);

            assertThat(response.sessionId()).isEqualTo(1L);
            assertThat(response.items()).hasSize(1);
            SessionHistoryResponse.HistoryItem item = response.items().getFirst();
            assertThat(item.questionText()).isEqualTo("如何设计限流？");
            assertThat(item.answerText()).isEqualTo("使用令牌桶");
            assertThat(item.evaluation().strengths()).containsExactly("表达清楚");
            assertThat(item.evaluation().score().accuracy()).isEqualTo(9);
        }

        @Test
        @DisplayName("无回答和评价时应返回空回答且不伪造评价")
        void shouldBuildHistoryWithoutAnswerOrEvaluation() {
            when(sessionMapper.selectById(1L)).thenReturn(buildSession(1L, InterviewState.READY, 0));
            when(questionMapper.selectList(any(QueryWrapper.class)))
                    .thenReturn(List.of(buildQuestion(10L, 1, "问题", "background")));
            when(answerMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);
            when(evaluationMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);

            SessionHistoryResponse response = interviewService.getSessionHistory(1L);

            assertThat(response.items().getFirst().answerText()).isEmpty();
            assertThat(response.items().getFirst().evaluation()).isNull();
        }
    }

    @Nested
    @DisplayName("generateMultiSessionSummary")
    class MultiSessionSummary {

        @Test
        @DisplayName("应校验 AI 结构化输出并返回跨会话总结")
        void shouldGenerateValidatedMultiSessionSummary() {
            InterviewSession first = buildSession(1L, InterviewState.COMPLETED, 3);
            first.setPersonaName("技术面试官");
            first.setPersonaTitle("架构师");
            InterviewSession second = buildSession(2L, InterviewState.COMPLETED, 3);
            second.setPersonaName("HR 面试官");
            second.setPersonaTitle("招聘经理");
            when(sessionMapper.selectById(1L)).thenReturn(first);
            when(sessionMapper.selectById(2L)).thenReturn(second);
            when(questionMapper.selectList(any(QueryWrapper.class)))
                    .thenReturn(List.of(buildQuestion(10L, 1, "问题", "technical")));
            when(answerMapper.selectOne(any(QueryWrapper.class)))
                    .thenReturn(buildAnswer(20L, 10L, "回答"));
            when(evaluationMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);
            when(aiClient.invoke(any())).thenReturn(AiResult.success("multi-1", """
                    {"overallScore":82,"overallSummary":"跨场次表现稳定", "crossStrengths":["技术基础"],
                     "crossWeaknesses":["量化表达"], "suggestions":["补充指标"]}
                    """, 100, 50, 200));

            MultiSessionSummaryResponse response = interviewService.generateMultiSessionSummary(
                    new MultiSessionSummaryRequest(List.of(1L, 2L)));

            assertThat(response.overallScore()).isEqualTo(82);
            assertThat(response.overallSummary()).isEqualTo("跨场次表现稳定");
            assertThat(response.crossStrengths()).containsExactly("技术基础");
            assertThat(response.sessions()).extracting(MultiSessionSummaryResponse.SessionBrief::personaName)
                    .containsExactly("技术面试官", "HR 面试官");
            verify(aiInvocationService).logInvocation(any(), any());
        }

        @Test
        @DisplayName("模型输出不满足结构化契约时应拒绝返回总结")
        void shouldRejectInvalidMultiSessionAiOutput() {
            when(sessionMapper.selectById(1L)).thenReturn(buildSession(1L, InterviewState.COMPLETED, 3));
            when(questionMapper.selectList(any(QueryWrapper.class))).thenReturn(List.of());
            when(aiClient.invoke(any())).thenReturn(AiResult.success("multi-bad", "{\"overallScore\":82}", 100, 50, 200));

            assertThatThrownBy(() -> interviewService.generateMultiSessionSummary(
                    new MultiSessionSummaryRequest(List.of(1L))))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("输出校验失败");
            verify(aiInvocationService).logInvocation(any(), any());
        }
    }

    // ── 辅助方法 ──

    private InterviewSession buildSession(Long id, InterviewState state, int currentIndex) {
        InterviewSession session = new InterviewSession();
        session.setId(id);
        session.setUserId(1L);
        session.setResumeVersionId(10L);
        session.setJobDescriptionId(20L);
        session.setStatus(state.name());
        session.setCurrentQuestionIndex(currentIndex);
        session.setTotalQuestions(3);
        session.setCreatedAt(LocalDateTime.now());
        return session;
    }

    private InterviewQuestion buildQuestion(Long id, int index, String text, String type) {
        InterviewQuestion q = new InterviewQuestion();
        q.setId(id);
        q.setSessionId(1L);
        q.setQuestionIndex(index);
        q.setQuestionText(text);
        q.setQuestionType(type);
        q.setCreatedAt(LocalDateTime.now());
        return q;
    }

    private InterviewAnswer buildAnswer(Long id, Long questionId, String text) {
        InterviewAnswer a = new InterviewAnswer();
        a.setId(id);
        a.setSessionId(1L);
        a.setQuestionId(questionId);
        a.setAnswerText(text);
        a.setCreatedAt(LocalDateTime.now());
        return a;
    }

    private InterviewEvaluation buildEvaluation(Long answerId, Long questionId) {
        InterviewEvaluation evaluation = new InterviewEvaluation();
        evaluation.setId(30L);
        evaluation.setSessionId(1L);
        evaluation.setQuestionId(questionId);
        evaluation.setAnswerId(answerId);
        evaluation.setScoreJson("{\"clarity\":6,\"relevance\":6,\"depth\":5,\"structure\":5,\"evidence\":4,\"accuracy\":4}");
        evaluation.setStrengthsJson("[\"回答已提交\"]");
        evaluation.setWeaknessesJson("[\"需要补充依据\"]");
        evaluation.setSuggestionsJson("[\"补充一个真实例子\"]");
        evaluation.setReferenceAnswerJson("");
        evaluation.setCreatedAt(LocalDateTime.now());
        return evaluation;
    }

    private JobDescription buildJd() {
        JobDescription jd = new JobDescription();
        jd.setId(20L);
        jd.setParsedJson("{\"title\":\"后端开发\",\"requirements\":[\"3年Java经验\"]}");
        return jd;
    }

    private InterviewerPersona buildPersona() {
        InterviewerPersona persona = new InterviewerPersona();
        persona.setId(1L);
        persona.setName("张老师");
        persona.setTitle("通用面试官");
        persona.setStyle("温和专业，善于引导候选人展现真实水平");
        persona.setType("preset");
        return persona;
    }

    private InterviewPlan buildPlan(Long id, LocalDateTime deletedAt) {
        InterviewPlan plan = new InterviewPlan();
        plan.setId(id);
        plan.setUserId(1L);
        plan.setDeletedAt(deletedAt);
        return plan;
    }
}
