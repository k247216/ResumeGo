package com.resumego.interview.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumego.ai.AiClient;
import com.resumego.ai.AiClientSelector;
import com.resumego.ai.AiErrorCategory;
import com.resumego.ai.AiInvocation;
import com.resumego.ai.AiInvocationMapper;
import com.resumego.ai.AiInvocationService;
import com.resumego.ai.AiRequest;
import com.resumego.ai.AiResult;
import com.resumego.ai.validate.AiOutputValidator;
import com.resumego.company.CompanyProfileService;
import com.resumego.common.CurrentUser;
import com.resumego.interview.feedback.InterviewFeedbackEvent;
import com.resumego.interview.feedback.InterviewFeedbackProjector;
import com.resumego.interview.InterviewAction;
import com.resumego.interview.InterviewMode;
import com.resumego.interview.QuestionSourceType;
import com.resumego.interview.context.InterviewContextSnapshot;
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
import com.resumego.interview.source.InterviewQuestionSource;
import com.resumego.interview.source.QuestionDraft;
import com.resumego.job.JobDescription;
import com.resumego.job.JobDescriptionMapper;
import com.resumego.resume.dto.ResumeVersionDTO;
import com.resumego.resume.repository.ResumeRepository;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 面试服务。
 * <p>
 * 核心职责：编排面试会话的完整生命周期，包括创建会话、开始面试、
 * 提交回答、评估和生成总结。状态机逻辑由 {@link InterviewStateMachine}
 * 控制，AI 仅用于生成内容（问题、评价、总结），不参与状态决策。
 * <p>
 * 安全约束：回答原文不写入普通日志；AI 输出经过结构化校验；
 * 状态转换使用确定性规则；AI 调用失败时重试一次后降级为 FAILED。
 */
@Service
public class InterviewService {

    private static final Logger log = LoggerFactory.getLogger(InterviewService.class);
    private static final String FEATURE_TYPE_QUESTION = "interview_question";
    private static final String FEATURE_TYPE_EVALUATION = "interview_evaluation";
    private static final String FEATURE_TYPE_SUMMARY = "interview_summary";
    private static final int DEFAULT_TOTAL_QUESTIONS = 5;
    /** 面试回答最大长度（字符数），防止超长文本导致 AI Token 超额 */
    private static final int MAX_ANSWER_LENGTH = 10_000;

    private final InterviewSessionMapper sessionMapper;
    private final InterviewQuestionMapper questionMapper;
    private final InterviewAnswerMapper answerMapper;
    private final InterviewEvaluationMapper evaluationMapper;
    private final InterviewPlanMapper planMapper;
    private final InterviewStateMachine stateMachine;
    private final InterviewPromptBuilder promptBuilder;
    private final InterviewerPersonaMapper personaMapper;
    private final AiClient aiClient;
    private final AiClientSelector aiClientSelector;
    private final AiInvocationService aiInvocationService;
    private final AiInvocationMapper aiInvocationMapper;
    private final AiOutputValidator outputValidator;
    private final ResumeRepository resumeRepository;
    private final JobDescriptionMapper jobDescriptionMapper;
    private final CompanyProfileService companyProfileService;
    private final ObjectMapper objectMapper;
    @Autowired(required = false)
    private List<InterviewQuestionSource> questionSources = List.of();

    @Autowired(required = false)
    private InterviewFeedbackProjector feedbackProjector;

    public InterviewService(
            InterviewSessionMapper sessionMapper,
            InterviewQuestionMapper questionMapper,
            InterviewAnswerMapper answerMapper,
            InterviewEvaluationMapper evaluationMapper,
            InterviewPlanMapper planMapper,
            InterviewStateMachine stateMachine,
            InterviewPromptBuilder promptBuilder,
            InterviewerPersonaMapper personaMapper,
            AiClientSelector aiClientSelector,
            AiInvocationService aiInvocationService,
            AiInvocationMapper aiInvocationMapper,
            AiOutputValidator outputValidator,
            ResumeRepository resumeRepository,
            JobDescriptionMapper jobDescriptionMapper,
            CompanyProfileService companyProfileService,
            ObjectMapper objectMapper) {
        this.sessionMapper = sessionMapper;
        this.questionMapper = questionMapper;
        this.answerMapper = answerMapper;
        this.evaluationMapper = evaluationMapper;
        this.planMapper = planMapper;
        this.stateMachine = stateMachine;
        this.promptBuilder = promptBuilder;
        this.personaMapper = personaMapper;
        this.aiClient = aiClientSelector.getClient();
        this.aiClientSelector = aiClientSelector;
        this.aiInvocationService = aiInvocationService;
        this.aiInvocationMapper = aiInvocationMapper;
        this.outputValidator = outputValidator;
        this.resumeRepository = resumeRepository;
        this.jobDescriptionMapper = jobDescriptionMapper;
        this.companyProfileService = companyProfileService;
        this.objectMapper = objectMapper;
    }

    // ── 公开 API ──

    /**
     * 创建面试会话。
     * <p>
     * 校验简历和岗位存在，创建会话并返回 READY 状态。
     *
     * @param request 包含 resumeVersionId 和 jobDescriptionId
     * @return 会话状态
     * @throws IllegalArgumentException 如果简历或岗位不存在
     */
    @Transactional
    public InterviewStatusResponse createInterview(StartInterviewRequest request) {
        // 校验简历存在
        ResumeVersionDTO resumeVersion = resumeRepository.findVersionById(request.resumeVersionId());
        if (resumeVersion == null) {
            throw new IllegalArgumentException("简历版本不存在: " + request.resumeVersionId());
        }

        // 校验岗位存在
        JobDescription jd = jobDescriptionMapper.selectById(request.jobDescriptionId());
        if (jd == null) {
            throw new IllegalArgumentException("岗位不存在: " + request.jobDescriptionId());
        }

        // 校验人设存在
        InterviewerPersona persona = personaMapper.selectById(request.personaId());
        if (persona == null) {
            throw new IllegalArgumentException("面试官人设不存在: " + request.personaId());
        }

        // 创建会话
        InterviewSession session = new InterviewSession();
        session.setUserId(CurrentUser.DEMO_USER_ID);
        session.setResumeVersionId(request.resumeVersionId());
        session.setJobDescriptionId(request.jobDescriptionId());
        session.setStatus(com.resumego.interview.InterviewState.READY.name());
        session.setCurrentQuestionIndex(0);
        session.setTotalQuestions(request.questionCount() != null ? request.questionCount() : DEFAULT_TOTAL_QUESTIONS);
        session.setPersonaId(persona.getId());
        session.setPersonaName(persona.getName());
        session.setPersonaTitle(persona.getTitle());
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());

        sessionMapper.insert(session);

        log.info("面试会话创建成功: sessionId={}, resumeVersionId={}, jdId={}, personaId={}",
                session.getId(), request.resumeVersionId(), request.jobDescriptionId(), persona.getId());

        return buildStatusResponse(session, null);
    }

    /**
     * Create a session whose questions come from a frozen Knowledge/Experience
     * source. These modes intentionally leave Pipeline/Resume references null;
     * the plan snapshot remains the source of truth for replay.
     */
    @Transactional
    public InterviewStatusResponse createSourceInterview(
            InterviewPlan plan,
            InterviewContextSnapshot snapshot,
            List<QuestionDraft> drafts) {
        if (plan == null || snapshot == null || drafts == null || drafts.isEmpty()) {
            throw new IllegalArgumentException("来源练习缺少可用题目");
        }

        Long personaId = snapshot.personaIds() == null || snapshot.personaIds().isEmpty()
                ? null : snapshot.personaIds().get(0);
        InterviewerPersona persona = personaId == null ? null : personaMapper.selectById(personaId);

        InterviewSession session = new InterviewSession();
        session.setUserId(CurrentUser.DEMO_USER_ID);
        session.setResumeVersionId(snapshot.resumeVersionId());
        session.setJobDescriptionId(snapshot.jobDescriptionId());
        session.setPlanId(plan.getId());
        session.setStatus(com.resumego.interview.InterviewState.READY.name());
        session.setCurrentQuestionIndex(0);
        session.setTotalQuestions(drafts.size());
        session.setPersonaId(personaId);
        session.setPersonaName(persona == null ? defaultPersonaName(snapshot.mode()) : persona.getName());
        session.setPersonaTitle(persona == null ? "AI 面试主持人" : persona.getTitle());
        session.setCreatedAt(LocalDateTime.now());
        session.setUpdatedAt(LocalDateTime.now());
        sessionMapper.insert(session);

        for (int index = 0; index < drafts.size(); index++) {
            QuestionDraft draft = drafts.get(index);
            InterviewQuestion question = new InterviewQuestion();
            question.setSessionId(session.getId());
            question.setQuestionIndex(index + 1);
            question.setQuestionText(draft.text());
            question.setQuestionType(normalizeQuestionType(draft.questionType()));
            question.setTargetSkill(draft.provenanceLabel());
            question.setSource(sourceCode(draft.sourceType()));
            question.setSourceReference(draft.sourceReference());
            question.setProvenanceLabel(draft.provenanceLabel());
            question.setCreatedAt(LocalDateTime.now());
            questionMapper.insert(question);
        }
        return buildStatusResponse(session, null);
    }

    private String defaultPersonaName(String mode) {
        return InterviewMode.KNOWLEDGE_TRAINING.name().equals(mode) ? "知识训练主持人" : "面经训练主持人";
    }

    private String normalizeQuestionType(String value) {
        if (value == null || value.isBlank()) return "other";
        String normalized = value.toLowerCase();
        if (Set.of("behavioral", "technical", "situational", "background", "other").contains(normalized)) {
            return normalized;
        }
        return "other";
    }

    private String sourceCode(QuestionSourceType sourceType) {
        if (sourceType == null) return "system_defined";
        return switch (sourceType) {
            case AI_GENERATED -> "ai_generated";
            case USER_MANUAL, IMPORTED_EXPERIENCE -> "manual";
            case GENERATED_PRACTICE, SYSTEM_DEFINED -> "system_defined";
            case AI_FOLLOW_UP -> "ai_follow_up";
        };
    }

    /**
     * 开始面试，触发 AI 生成第一题。
     * <p>
     * 流程：READY → ASKING → 生成问题 → WAITING_ANSWER
     *
     * @param sessionId 会话 ID
     * @return 会话状态（包含第一题）
     * @throws IllegalArgumentException 如果会话不存在
     * @throws IllegalStateException    如果当前状态不允许开始
     */
    @Transactional
    public InterviewStatusResponse startInterview(Long sessionId) {
        InterviewSession session = loadSession(sessionId);

        // 0. AI 未配置时明确拒绝，避免静默生成空面试
        if (!aiClientSelector.isConfigured()) {
            throw new IllegalStateException("尚未配置 AI 模型服务，请先在设置页添加模型服务");
        }

        // 1. 先校验状态，避免非法状态下产生多余 AI 调用
        if (!stateMachine.canTransition(session, InterviewAction.START)) {
            throw new IllegalStateException("当前状态不允许开始面试: " + session.getStatus());
        }

        // 2. READY → ASKING
        stateMachine.transition(session, InterviewAction.START);
        session.setUpdatedAt(LocalDateTime.now());
        sessionMapper.updateById(session);

        // 3. 生成第一题（AI 失败则标记 FAILED）
        InterviewQuestion question = isSourceSession(session)
                ? findQuestionEntityByIndex(session.getId(), 1)
                : generateQuestion(session);
        if (question == null) {
            stateMachine.markFailed(session);
            session.setUpdatedAt(LocalDateTime.now());
            sessionMapper.updateById(session);
            return buildStatusResponse(session, null);
        }

        // 4. ASKING → WAITING_ANSWER
        stateMachine.transition(session, InterviewAction.QUESTION_READY);
        session.setCurrentQuestionIndex(1);
        session.setUpdatedAt(LocalDateTime.now());
        sessionMapper.updateById(session);

        InterviewQuestionDTO questionDTO = toQuestionDTO(question);
        return buildStatusResponse(session, questionDTO);
    }

    /**
     * 列出当前用户的所有面试会话。
     *
     * @return 会话列表（按创建时间倒序）
     */
    public List<InterviewStatusResponse> listMyInterviews() {
        QueryWrapper<InterviewSession> query = new QueryWrapper<>();
        query.eq("user_id", CurrentUser.DEMO_USER_ID)
                .orderByDesc("created_at");
        List<InterviewSession> sessions = sessionMapper.selectList(query);
        Set<Long> visiblePlanIds = loadVisiblePlanIds(sessions);

        List<InterviewStatusResponse> result = new ArrayList<>();
        for (InterviewSession session : sessions) {
            if (session.getPlanId() != null && !visiblePlanIds.contains(session.getPlanId())) {
                continue;
            }
            InterviewQuestionDTO currentQuestion = null;
            int currentIndex = session.getCurrentQuestionIndex() != null
                    ? session.getCurrentQuestionIndex() : 0;
            if (currentIndex > 0) {
                currentQuestion = findQuestionByIndex(session.getId(), currentIndex);
            }
            result.add(buildStatusResponse(session, currentQuestion));
        }
        return result;
    }

    private Set<Long> loadVisiblePlanIds(List<InterviewSession> sessions) {
        List<Long> planIds = sessions.stream()
                .map(InterviewSession::getPlanId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (planIds.isEmpty()) {
            return Set.of();
        }
        List<InterviewPlan> plans = planMapper.selectBatchIds(planIds);
        return plans.stream()
                .filter(plan -> Objects.equals(plan.getUserId(), CurrentUser.DEMO_USER_ID))
                .filter(plan -> plan.getDeletedAt() == null)
                .map(InterviewPlan::getId)
                .collect(java.util.stream.Collectors.toSet());
    }

    /**
     * 获取会话的完整问答历史（含评价），用于前端恢复会话。
     *
     * @param sessionId 会话 ID
     * @return 问答历史
     * @throws IllegalArgumentException 如果会话不存在
     */
    public SessionHistoryResponse getSessionHistory(Long sessionId) {
        InterviewSession session = loadSession(sessionId);

        QueryWrapper<InterviewQuestion> qQuery = new QueryWrapper<>();
        qQuery.eq("session_id", sessionId).orderByAsc("question_index");
        List<InterviewQuestion> questions = questionMapper.selectList(qQuery);

        List<SessionHistoryResponse.HistoryItem> items = new ArrayList<>();
        for (InterviewQuestion q : questions) {
            // 查询回答
            QueryWrapper<InterviewAnswer> aQuery = new QueryWrapper<>();
            aQuery.eq("question_id", q.getId());
            InterviewAnswer answer = answerMapper.selectOne(aQuery);

            // 查询评价
            QueryWrapper<InterviewEvaluation> eQuery = new QueryWrapper<>();
            eQuery.eq("question_id", q.getId());
            InterviewEvaluation evaluation = evaluationMapper.selectOne(eQuery);

            SessionHistoryResponse.EvaluationSummary evalSummary = null;
            if (evaluation != null) {
                Map<String, Integer> scoreMap = parseScoreJson(evaluation.getScoreJson());
                evalSummary = new SessionHistoryResponse.EvaluationSummary(
                        parseJsonArray(evaluation.getStrengthsJson()),
                        parseJsonArray(evaluation.getWeaknessesJson()),
                        parseJsonArray(evaluation.getSuggestionsJson()),
                        evaluation.getReferenceAnswerJson(),
                        new SessionHistoryResponse.ScoreDetail(
                                scoreMap.getOrDefault("clarity", 0),
                                scoreMap.getOrDefault("relevance", 0),
                                scoreMap.getOrDefault("depth", 0),
                                scoreMap.getOrDefault("structure", 0),
                                scoreMap.containsKey("evidence")
                                        ? scoreMap.get("evidence")
                                        : scoreMap.getOrDefault("accuracy", 0),
                                scoreMap.getOrDefault("accuracy", 0)
                        )
                );
            }

            items.add(new SessionHistoryResponse.HistoryItem(
                    q.getQuestionIndex() != null ? q.getQuestionIndex() : 0,
                    q.getQuestionText(),
                    q.getQuestionType(),
                    answer != null ? answer.getAnswerText() : "",
                    evalSummary,
                    q.getSource(),
                    q.getSourceReference(),
                    q.getProvenanceLabel(),
                    answer != null ? answer.getCreatedAt() : null
            ));
        }

        return new SessionHistoryResponse(session.getId(), items);
    }

    /**
     * 查询面试状态。
     *
     * @param sessionId 会话 ID
     * @return 当前状态和当前问题
     * @throws IllegalArgumentException 如果会话不存在
     */
    public InterviewStatusResponse getInterviewStatus(Long sessionId) {
        InterviewSession session = loadSession(sessionId);

        InterviewQuestionDTO currentQuestion = null;
        int currentIndex = session.getCurrentQuestionIndex() != null
                ? session.getCurrentQuestionIndex() : 0;
        if (currentIndex > 0) {
            currentQuestion = findQuestionByIndex(sessionId, currentIndex);
        }

        return buildStatusResponse(session, currentQuestion);
    }

    /**
     * 提交回答。
     * <p>
     * 流程：WAITING_ANSWER → 保存回答 → EVALUATING → 评价 → 决定下一步
     * <ul>
     *   <li>非最后一题：生成下一题 → ASKING → WAITING_ANSWER</li>
     *   <li>最后一题：生成总结 → SUMMARIZING → COMPLETED</li>
     * </ul>
     *
     * @param sessionId 会话 ID
     * @param request   包含回答文本
     * @return 包含评价摘要和下一题（如有）
     * @throws IllegalArgumentException 如果会话不存在
     * @throws IllegalStateException    如果当前状态不允许提交回答
     */
    @Transactional
    public SubmitAnswerResponse submitAnswer(Long sessionId, SubmitAnswerRequest request) {
        InterviewSession session = loadSession(sessionId);

        // 校验：回答内容长度
        String answerText = request.answerText();
        if (answerText == null || answerText.isBlank()) {
            throw new IllegalArgumentException("回答内容不能为空");
        }
        if (answerText.length() > MAX_ANSWER_LENGTH) {
            throw new IllegalArgumentException(
                    "回答内容过长，当前 " + answerText.length() + " 字符，最大允许 " + MAX_ANSWER_LENGTH + " 字符");
        }

        // 1. 校验状态
        if (!stateMachine.canTransition(session, InterviewAction.ANSWER_SUBMITTED)) {
            throw new IllegalStateException("当前状态不允许提交回答: " + session.getStatus());
        }

        int currentIndex = session.getCurrentQuestionIndex();

        // 2. 查找当前问题
        InterviewQuestion currentQuestion = findQuestionEntityByIndex(sessionId, currentIndex);
        if (currentQuestion == null) {
            throw new IllegalStateException("当前问题不存在: sessionId=" + sessionId + ", index=" + currentIndex);
        }

        // 3. 保存或复用回答（评价失败后的重试不能重复插入同一题回答）
        InterviewAnswer answer = findAnswerByQuestionId(currentQuestion.getId());
        if (answer == null) {
            answer = new InterviewAnswer();
            answer.setSessionId(sessionId);
            answer.setQuestionId(currentQuestion.getId());
            answer.setAnswerText(request.answerText());
            answer.setCreatedAt(LocalDateTime.now());
            answerMapper.insert(answer);
            log.info("回答已保存: sessionId={}, questionIndex={}, answerId={}",
                    sessionId, currentIndex, answer.getId());
        } else {
            log.info("复用已保存回答进行评价重试: sessionId={}, questionIndex={}, answerId={}",
                    sessionId, currentIndex, answer.getId());
        }

        // 4. 评价回答（先调 AI，失败则返回重试标记）
        EvaluationAttempt evaluationAttempt = evaluateAnswer(session, currentQuestion, answer);
        InterviewEvaluation evaluation = evaluationAttempt.evaluation();
        if (evaluation == null) {
            return buildSubmitResponse(session, null, null, false, true,
                    evaluationAttempt.errorCategory(), evaluationAttempt.errorMessage());
        }

        // 5. WAITING_ANSWER → EVALUATING
        stateMachine.transition(session, InterviewAction.ANSWER_SUBMITTED);
        session.setUpdatedAt(LocalDateTime.now());
        sessionMapper.updateById(session);

        // 6. 判断下一步
        InterviewAction nextAction = stateMachine.determinePostEvaluationAction(session);

        if (nextAction == InterviewAction.GO_SUMMARIZE) {
            // 最后一题：进入总结
            return handleSummary(session, evaluation);
        } else {
            // 非最后一题：生成下一题
            return handleNextQuestion(session, evaluation);
        }
    }

    // ── 私有辅助方法 ──

    /**
     * 加载会话，不存在则抛异常。
     */
    private InterviewSession loadSession(Long sessionId) {
        InterviewSession session = sessionMapper.selectById(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("面试会话不存在: " + sessionId);
        }
        return session;
    }

    /**
     * 根据题号查找问题。
     */
    private InterviewQuestionDTO findQuestionByIndex(Long sessionId, int questionIndex) {
        QueryWrapper<InterviewQuestion> query = new QueryWrapper<>();
        query.eq("session_id", sessionId)
                .eq("question_index", questionIndex);
        InterviewQuestion q = questionMapper.selectOne(query);
        return q != null ? toQuestionDTO(q) : null;
    }

    /**
     * 查找完整的 Question 实体。
     */
    private InterviewQuestion findQuestionEntityByIndex(Long sessionId, int questionIndex) {
        QueryWrapper<InterviewQuestion> query = new QueryWrapper<>();
        query.eq("session_id", sessionId)
                .eq("question_index", questionIndex);
        return questionMapper.selectOne(query);
    }

    /**
     * 根据问题 ID 查找已保存回答。
     * <p>
     * interview_answers.question_id 有唯一约束；评价失败重试时应复用回答，
     * 避免重复插入导致唯一键冲突。
     */
    private InterviewAnswer findAnswerByQuestionId(Long questionId) {
        QueryWrapper<InterviewAnswer> query = new QueryWrapper<>();
        query.eq("question_id", questionId);
        return answerMapper.selectOne(query);
    }

    /**
     * 生成面试问题（含重试）。
     */
    private InterviewQuestion generateQuestion(InterviewSession session) {
        int nextIndex = (session.getCurrentQuestionIndex() != null
                ? session.getCurrentQuestionIndex() : 0) + 1;

        // 加载简历和 JD
        Map<String, Object> resumeContent = loadResumeContent(session.getResumeVersionId());
        Map<String, Object> jdContent = loadJdContent(session.getJobDescriptionId());
        Map<String, Object> companyProfile = loadCompanyProfile(session.getJobDescriptionId());

        // 收集已问过的问题
        List<Map<String, String>> previousQuestions = loadPreviousQuestions(session.getId());

        // 加载人设
        Map<String, String> personaContext = buildPersonaContext(session);

        // 构建 Prompt
        String systemPrompt = promptBuilder.buildQuestionSystemPrompt(personaContext);
        String userMessage = promptBuilder.buildQuestionUserMessage(
                resumeContent, jdContent, nextIndex, session.getTotalQuestions(), previousQuestions, companyProfile);

        // 调用 AI（含重试）
        AiResult aiResult = invokeAiWithRetry(
                FEATURE_TYPE_QUESTION, systemPrompt, userMessage, session);

        if (!aiResult.success()) {
            stateMachine.markFailed(session);
            session.setUpdatedAt(LocalDateTime.now());
            sessionMapper.updateById(session);
            log.warn("问题生成失败，面试已终止: sessionId={}", session.getId());
            return null;
        }

        // 校验并解析 AI 输出
        Map<String, Object> parsed = parseAndValidateQuestionOutput(aiResult);
        if (parsed == null) {
            stateMachine.markFailed(session);
            session.setUpdatedAt(LocalDateTime.now());
            sessionMapper.updateById(session);
            log.warn("AI 问题输出校验失败，面试已终止: sessionId={}", session.getId());
            return null;
        }

        // 保存问题
        InterviewQuestion question = new InterviewQuestion();
        question.setSessionId(session.getId());
        question.setQuestionIndex(nextIndex);
        question.setQuestionText((String) parsed.get("questionText"));
        question.setQuestionType((String) parsed.get("questionType"));
        question.setTargetSkill((String) parsed.get("targetSkill"));
        // MySQL 将 source 定义为 NOT NULL，且历史会话需要区分 AI 生成题与
        // 知识库/面经题。此前这里写入 null，导致首题之后生成下一题时整笔
        // 事务回滚，用户只能看到“服务内部错误”，已提交回答和评价也随之消失。
        question.setSource("ai_generated");
        question.setCreatedAt(LocalDateTime.now());

        // 关联 AI 调用记录
        AiInvocation invocation = findInvocationByRequestId(aiResult.requestId());
        if (invocation != null) {
            question.setGenerationAiInvocationId(invocation.getId());
        }

        questionMapper.insert(question);
        log.info("问题已生成: sessionId={}, questionIndex={}, questionId={}",
                session.getId(), nextIndex, question.getId());

        return question;
    }

    /**
     * 评价回答（含重试）。
     */
    private EvaluationAttempt evaluateAnswer(InterviewSession session,
                                             InterviewQuestion question,
                                             InterviewAnswer answer) {
        // 回答和评价通过 answer_id 建立唯一关系。用户在网络超时、刷新或重复点击后
        // 重试时，先复用已经落库的评价，避免再次插入触发唯一键异常并返回 500。
        InterviewEvaluation existingEvaluation = findEvaluationByAnswerId(answer.getId());
        if (existingEvaluation != null) {
            log.info("复用已保存评价: sessionId={}, questionIndex={}, evaluationId={}",
                    session.getId(), question.getQuestionIndex(), existingEvaluation.getId());
            return EvaluationAttempt.success(existingEvaluation);
        }

        Map<String, Object> jdContent = loadJdContent(session.getJobDescriptionId());
        Map<String, Object> resumeContent = loadResumeContent(session.getResumeVersionId());
        Map<String, Object> companyProfile = loadCompanyProfile(session.getJobDescriptionId());
        Map<String, String> personaContext = buildPersonaContext(session);

        String systemPrompt = promptBuilder.buildEvaluationSystemPrompt(personaContext);
        String userMessage = promptBuilder.buildEvaluationUserMessage(
                question.getQuestionText(), answer.getAnswerText(), jdContent, resumeContent, companyProfile);

        AiResult aiResult = invokeAiWithRetry(
                FEATURE_TYPE_EVALUATION, systemPrompt, userMessage, session);

        if (!aiResult.success()) {
            log.warn("回答评价失败，可重试: sessionId={}", session.getId());
            return EvaluationAttempt.failed(aiResult.errorCategory(), aiResult.errorMessage());
        }

        // 校验并解析 AI 输出
        Map<String, Object> parsed = parseAndValidateEvaluationOutput(aiResult);
        if (parsed == null) {
            log.warn("AI 评价输出校验失败，可重试: sessionId={}", session.getId());
            return EvaluationAttempt.failed(AiErrorCategory.INVALID_JSON,
                    "模型返回的评价格式无法识别，请重试或检查模型配置");
        }

        // 保存评价
        InterviewEvaluation evaluation = new InterviewEvaluation();
        evaluation.setSessionId(session.getId());
        evaluation.setQuestionId(question.getId());
        evaluation.setAnswerId(answer.getId());
        evaluation.setScoreJson(toJsonString(parsed.get("score")));
        evaluation.setStrengthsJson(toJsonString(parsed.get("strengths")));
        evaluation.setWeaknessesJson(toJsonString(parsed.get("weaknesses")));
        evaluation.setSuggestionsJson(toJsonString(parsed.get("suggestions")));
        evaluation.setReferenceAnswerJson((String) parsed.get("referenceAnswer"));
        evaluation.setCreatedAt(LocalDateTime.now());

        AiInvocation invocation = findInvocationByRequestId(aiResult.requestId());
        if (invocation != null) {
            evaluation.setEvaluationAiInvocationId(invocation.getId());
        }

        evaluationMapper.insert(evaluation);
        log.info("评价已保存: sessionId={}, questionIndex={}, evaluationId={}",
                session.getId(), question.getQuestionIndex(), evaluation.getId());

        return EvaluationAttempt.success(evaluation);
    }

    /** 根据回答 ID 查找已落库评价，支持评价请求幂等重试。 */
    private InterviewEvaluation findEvaluationByAnswerId(Long answerId) {
        if (answerId == null) return null;
        QueryWrapper<InterviewEvaluation> query = new QueryWrapper<>();
        query.eq("answer_id", answerId);
        return evaluationMapper.selectOne(query);
    }

    private record EvaluationAttempt(InterviewEvaluation evaluation,
                                     AiErrorCategory errorCategory,
                                     String errorMessage) {
        private static EvaluationAttempt success(InterviewEvaluation evaluation) {
            return new EvaluationAttempt(evaluation, null, null);
        }

        private static EvaluationAttempt failed(AiErrorCategory category, String message) {
            return new EvaluationAttempt(null,
                    category == null ? AiErrorCategory.PROVIDER_ERROR : category,
                    message == null || message.isBlank() ? "模型服务调用失败，请稍后重试" : message);
        }
    }

    /**
     * 处理非最后一题：生成下一题，返回当前评价和下一题。
     */
    private SubmitAnswerResponse handleNextQuestion(InterviewSession session,
                                                     InterviewEvaluation evaluation) {
        // 生成下一题（先调 AI，失败则直接返回，markFailed 已在 generateQuestion 中调用）
        int nextIndex = (session.getCurrentQuestionIndex() != null ? session.getCurrentQuestionIndex() : 0) + 1;
        InterviewQuestion nextQuestion = isSourceSession(session)
                ? findQuestionEntityByIndex(session.getId(), nextIndex)
                : generateQuestion(session);
        if (nextQuestion == null) {
            return buildSubmitResponse(session, null, evaluation,
                    isTerminalStatus(session.getStatus()), false);
        }

        // 更新题号
        session.setCurrentQuestionIndex(nextQuestion.getQuestionIndex());

        // EVALUATING → ASKING → WAITING_ANSWER
        stateMachine.transition(session, InterviewAction.GO_NEXT);
        stateMachine.transition(session, InterviewAction.QUESTION_READY);
        session.setUpdatedAt(LocalDateTime.now());
        sessionMapper.updateById(session);

        return buildSubmitResponse(session, nextQuestion, evaluation, false, false);
    }

    private boolean isSourceSession(InterviewSession session) {
        if (session == null || session.getPlanId() == null) return false;
        InterviewPlan plan = planMapper.selectById(session.getPlanId());
        return plan != null && (InterviewMode.KNOWLEDGE_TRAINING.name().equals(plan.getMode())
                || InterviewMode.EXPERIENCE_SIMULATION.name().equals(plan.getMode()));
    }

    /**
     * 处理最后一题：生成总结，完成面试。
     */
    private SubmitAnswerResponse handleSummary(InterviewSession session,
                                                InterviewEvaluation evaluation) {
        // 生成总结（先调 AI，失败则直接返回，markFailed 已在 generateSummary 中调用）
        String summaryJson = generateSummary(session);
        if (summaryJson == null) {
            return buildSubmitResponse(session, null, evaluation,
                    isTerminalStatus(session.getStatus()), false);
        }

        // EVALUATING → SUMMARIZING → COMPLETED
        stateMachine.transition(session, InterviewAction.GO_SUMMARIZE);
        session.setUpdatedAt(LocalDateTime.now());
        sessionMapper.updateById(session);

        stateMachine.transition(session, InterviewAction.SUMMARY_READY);
        session.setSummaryJson(summaryJson);
        session.setUpdatedAt(LocalDateTime.now());
        sessionMapper.updateById(session);

        log.info("面试已完成: sessionId={}", session.getId());

        return buildSubmitResponse(session, null, evaluation, true, false);
    }

    /**
     * 生成面试总结（含重试）。
     */
    private String generateSummary(InterviewSession session) {
        // 收集所有问答和评价
        List<Map<String, Object>> qaList = buildQaList(session.getId());
        Map<String, String> personaContext = buildPersonaContext(session);
        Map<String, Object> companyProfile = loadCompanyProfile(session.getJobDescriptionId());

        String systemPrompt = promptBuilder.buildSummarySystemPrompt(personaContext);
        String userMessage = promptBuilder.buildSummaryUserMessage(qaList, companyProfile);

        AiResult aiResult = invokeAiWithRetry(
                FEATURE_TYPE_SUMMARY, systemPrompt, userMessage, session);

        if (!aiResult.success()) {
            stateMachine.markFailed(session);
            session.setUpdatedAt(LocalDateTime.now());
            sessionMapper.updateById(session);
            log.warn("总结生成失败，面试已终止: sessionId={}", session.getId());
            return null;
        }

        // 校验并解析 AI 输出
        Map<String, Object> parsed = parseAndValidateSummaryOutput(aiResult);
        if (parsed == null) {
            stateMachine.markFailed(session);
            session.setUpdatedAt(LocalDateTime.now());
            sessionMapper.updateById(session);
            log.warn("AI 总结输出校验失败，面试已终止: sessionId={}", session.getId());
            return null;
        }
        return toJsonString(parsed);
    }

    // ── AI 调用 ──

    /**
     * 调用 AI 并重试一次。
     * <p>
     * 每次调用都写入审计日志。重试使用新的 requestId。
     */
    private AiResult invokeAiWithRetry(String featureType, String systemPrompt,
                                        String userMessage, InterviewSession session) {
        // 第一次尝试
        AiResult result = invokeAi(featureType, systemPrompt, userMessage);
        if (result.success()) {
            return result;
        }

        log.warn("AI 调用失败，准备重试: sessionId={}, featureType={}, errorCategory={}",
                session != null ? session.getId() : "N/A", featureType, result.errorCategory());

        // 重试
        AiResult retryResult = invokeAi(featureType, systemPrompt, userMessage);
        if (retryResult.success()) {
            log.info("AI 重试成功: sessionId={}, featureType={}", session.getId(), featureType);
        }
        return retryResult;
    }

    /**
     * 执行单次 AI 调用并写入审计日志。
     */
    private AiResult invokeAi(String featureType, String systemPrompt, String userMessage) {
        String requestId = UUID.randomUUID().toString();
        AiRequest aiRequest = AiRequest.builder()
                .requestId(requestId)
                .featureType(featureType)
                .userId(CurrentUser.DEMO_USER_ID)
                .promptVersion(InterviewPromptBuilder.PROMPT_VERSION)
                .systemPrompt(systemPrompt)
                .userMessage(userMessage)
                .build();

        long startTime = System.currentTimeMillis();
        AiResult aiResult;
        try {
            aiResult = aiClient.invoke(aiRequest);
        } catch (Exception e) {
            log.error("AI 调用异常: requestId={}, featureType={}", requestId, featureType, e);
            aiResult = AiResult.failure(requestId, AiErrorCategory.PROVIDER_ERROR,
                    "AI 服务异常，请稍后重试", System.currentTimeMillis() - startTime);
        }

        aiInvocationService.logInvocation(aiRequest, aiResult);
        return aiResult;
    }

    // ── AI 输出校验 ──

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseAndValidateQuestionOutput(AiResult aiResult) {
        try {
            String json = outputValidator.extractJson(aiResult.content());
            var jsonResult = outputValidator.validateJson(json);
            if (!jsonResult.isValid()) {
                log.warn("AI 问题输出 JSON 格式非法: requestId={}", aiResult.requestId());
                return null;
            }

            var fieldResult = outputValidator.validateRequiredFields(json,
                    List.of("questionText", "questionType", "targetSkill"));
            if (!fieldResult.isValid()) {
                log.warn("AI 问题输出缺少必填字段: requestId={}, errors={}",
                        aiResult.requestId(), fieldResult.errors());
                return null;
            }

            // 校验 questionType 值域
            var typeResult = outputValidator.validateFieldValue(json, "questionType",
                    Set.of("behavioral", "technical", "situational", "background", "other"));
            if (!typeResult.isValid()) {
                log.warn("AI 问题输出 questionType 值域非法: requestId={}", aiResult.requestId());
                return null;
            }

            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("AI 问题输出解析失败: requestId={}", aiResult.requestId(), e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseAndValidateEvaluationOutput(AiResult aiResult) {
        try {
            String json = outputValidator.extractJson(aiResult.content());
            var jsonResult = outputValidator.validateJson(json);
            if (!jsonResult.isValid()) {
                log.warn("AI 评价输出 JSON 格式非法: requestId={}", aiResult.requestId());
                return null;
            }

            var fieldResult = outputValidator.validateRequiredFields(json,
                    List.of("score", "strengths", "weaknesses", "suggestions"));
            if (!fieldResult.isValid()) {
                log.warn("AI 评价输出缺少必填字段: requestId={}, errors={}",
                        aiResult.requestId(), fieldResult.errors());
                return null;
            }

            // 校验 score 为对象
            var scoreTypeResult = outputValidator.validateFieldType(json, "score", "object");
            if (!scoreTypeResult.isValid()) {
                log.warn("AI 评价输出 score 字段类型错误: requestId={}", aiResult.requestId());
                return null;
            }

            // 校验 strengths / weaknesses / suggestions 为数组
            for (String arrayField : new String[]{"strengths", "weaknesses", "suggestions"}) {
                var arrayResult = outputValidator.validateFieldType(json, arrayField, "array");
                if (!arrayResult.isValid()) {
                    log.warn("AI 评价输出 {} 字段类型错误（非数组）: requestId={}", arrayField, aiResult.requestId());
                    return null;
                }
            }

            Map<String, Object> parsed = objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
            // referenceAnswer 对知识训练/真题演练不是必需字段；缺失时归一化为空，
            // 避免模型省略“无简历上下文”的示范回答导致整次评价失败。
            return normalizeEvaluationPayload(parsed);
        } catch (Exception e) {
            log.warn("AI 评价输出解析失败: requestId={}", aiResult.requestId(), e);
            return null;
        }
    }

    /**
     * Provider 可能把评分返回为字符串或小数。统一保存为 1-10 的整数，
     * 这样提交响应、历史复盘和成长趋势都使用同一份可解释数据。
     */
    private Map<String, Object> normalizeEvaluationPayload(Map<String, Object> parsed) {
        Object rawScore = parsed.get("score");
        if (!(rawScore instanceof Map<?, ?> scoreObject)) {
            return null;
        }
        Map<String, Integer> score = new HashMap<>();
        for (String key : List.of("clarity", "relevance", "depth")) {
            Integer normalized = normalizeScoreValue(scoreObject.get(key));
            if (normalized == null) {
                log.warn("AI 评价输出评分字段无效: {}", key);
                return null;
            }
            score.put(key, normalized);
        }
        // 新版五维评分与主页/复盘保持一致。旧模型仍可能只返回 accuracy，
        // 此时将其作为 evidence 的兼容回退，并把缺失的 structure 保留为 0，
        // 由前端按“暂无该维度数据”处理，不虚构用户能力。
        Integer structure = normalizeScoreValue(scoreObject.get("structure"));
        if (structure == null) structure = 0;
        Integer evidence = normalizeScoreValue(scoreObject.get("evidence"));
        if (evidence == null) evidence = normalizeScoreValue(scoreObject.get("accuracy"));
        if (evidence == null) {
            log.warn("AI 评价输出评分字段无效: evidence/accuracy");
            return null;
        }
        score.put("structure", structure);
        score.put("evidence", evidence);
        // 保留旧字段，便于历史客户端继续读取；它与 evidence 始终保持同值。
        score.put("accuracy", evidence);
        parsed.put("score", score);

        for (String field : List.of("strengths", "weaknesses", "suggestions")) {
            Object value = parsed.get(field);
            if (!(value instanceof List<?> items)) {
                return null;
            }
            List<String> normalizedItems = new ArrayList<>();
            for (Object item : items) {
                if (!(item instanceof String text)) return null;
                String normalized = text.trim();
                if (!normalized.isEmpty()) normalizedItems.add(normalized);
            }
            parsed.put(field, normalizedItems);
        }

        Object referenceAnswer = parsed.get("referenceAnswer");
        if (referenceAnswer == null) {
            // 知识训练/真题演练可能没有简历上下文；空字符串表示不生成虚构参考经历。
            parsed.put("referenceAnswer", "");
        } else if (!(referenceAnswer instanceof String)) {
            return null;
        }
        return parsed;
    }

    private Integer normalizeScoreValue(Object value) {
        if (value == null) return null;
        try {
            double numeric;
            if (value instanceof Number number) {
                numeric = number.doubleValue();
            } else {
                String raw = value.toString().trim();
                try {
                    numeric = Double.parseDouble(raw);
                } catch (NumberFormatException ignored) {
                    // Models occasionally add “分” or return “8/10”. Extract only
                    // the numeric score; the range normalization below remains the
                    // single source of truth for persisted dimensions.
                    Matcher matcher = Pattern.compile("(?<!\\d)(\\d+(?:\\.\\d+)?)").matcher(raw);
                    if (!matcher.find()) return null;
                    numeric = Double.parseDouble(matcher.group(1));
                }
            }
            if (!Double.isFinite(numeric)) return null;
            // The prompt asks for 1–10, but a few compatible models still answer
            // with a percentage. Convert 0–100 scores to the same persisted scale
            // instead of discarding an otherwise valid evaluation.
            if (numeric > 10 && numeric <= 100) numeric /= 10.0;
            int rounded = (int) Math.round(numeric);
            return rounded >= 1 && rounded <= 10 ? rounded : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseAndValidateSummaryOutput(AiResult aiResult) {
        try {
            String json = outputValidator.extractJson(aiResult.content());
            var jsonResult = outputValidator.validateJson(json);
            if (!jsonResult.isValid()) {
                log.warn("AI 总结输出 JSON 格式非法: requestId={}", aiResult.requestId());
                return null;
            }

            var fieldResult = outputValidator.validateRequiredFields(json,
                    List.of("overallScore", "dimensionScores", "strengths", "weaknesses", "suggestions"));
            if (!fieldResult.isValid()) {
                log.warn("AI 总结输出缺少必填字段: requestId={}, errors={}",
                        aiResult.requestId(), fieldResult.errors());
                return null;
            }

            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("AI 总结输出解析失败: requestId={}", aiResult.requestId(), e);
            return null;
        }
    }

    // ── 数据加载 ──

    private Map<String, String> buildPersonaContext(InterviewSession session) {
        if (session.getPersonaId() == null) {
            return Map.of();
        }
        InterviewerPersona persona = personaMapper.selectById(session.getPersonaId());
        if (persona == null) {
            return Map.of();
        }
        Map<String, String> context = new HashMap<>();
        context.put("name", persona.getName());
        context.put("title", persona.getTitle());
        context.put("style", persona.getStyle());
        return context;
    }

    private Map<String, Object> loadResumeContent(Long resumeVersionId) {
        if (resumeVersionId == null) {
            return Map.of();
        }
        ResumeVersionDTO version = resumeRepository.findVersionById(resumeVersionId);
        if (version == null || version.content() == null) {
            return Map.of();
        }
        return version.content();
    }

    private Map<String, Object> loadJdContent(Long jdId) {
        if (jdId == null) {
            return Map.of();
        }
        JobDescription jd = jobDescriptionMapper.selectById(jdId);
        if (jd == null || jd.getParsedJson() == null) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(jd.getParsedJson(),
                    new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("JD parsedJson 解析失败: jdId={}", jdId, e);
            return Map.of();
        }
    }

    private Map<String, Object> loadCompanyProfile(Long jdId) {
        if (jdId == null) {
            return Map.of();
        }
        JobDescription jd = jobDescriptionMapper.selectById(jdId);
        if (jd == null) {
            return Map.of();
        }
        return companyProfileService.findEnabledProfileByCompanyName(jd.getCompanyName());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> firstAvailableCompanyProfile(List<Map<String, Object>> sessionSummaries) {
        if (sessionSummaries == null) {
            return Map.of();
        }
        for (Map<String, Object> sessionSummary : sessionSummaries) {
            Object profile = sessionSummary.get("companyProfile");
            if (profile instanceof Map<?, ?> map && !map.isEmpty()) {
                return (Map<String, Object>) map;
            }
        }
        return Map.of();
    }

    private List<Map<String, String>> loadPreviousQuestions(Long sessionId) {
        QueryWrapper<InterviewQuestion> query = new QueryWrapper<>();
        query.eq("session_id", sessionId)
                .orderByAsc("question_index");
        List<InterviewQuestion> questions = questionMapper.selectList(query);

        List<Map<String, String>> result = new ArrayList<>();
        for (InterviewQuestion q : questions) {
            Map<String, String> item = new HashMap<>();
            item.put("questionText", q.getQuestionText());
            item.put("questionType", q.getQuestionType() != null ? q.getQuestionType() : "");
            result.add(item);
        }
        return result;
    }

    private List<Map<String, Object>> buildQaList(Long sessionId) {
        // 查询所有问题
        QueryWrapper<InterviewQuestion> qQuery = new QueryWrapper<>();
        qQuery.eq("session_id", sessionId).orderByAsc("question_index");
        List<InterviewQuestion> questions = questionMapper.selectList(qQuery);

        List<Map<String, Object>> qaList = new ArrayList<>();
        for (InterviewQuestion q : questions) {
            Map<String, Object> qa = new HashMap<>();
            qa.put("questionText", q.getQuestionText());

            // 查询对应回答
            QueryWrapper<InterviewAnswer> aQuery = new QueryWrapper<>();
            aQuery.eq("question_id", q.getId());
            InterviewAnswer answer = answerMapper.selectOne(aQuery);
            qa.put("answerText", answer != null ? answer.getAnswerText() : "");

            // 查询对应评价
            QueryWrapper<InterviewEvaluation> eQuery = new QueryWrapper<>();
            eQuery.eq("question_id", q.getId());
            InterviewEvaluation evaluation = evaluationMapper.selectOne(eQuery);
            if (evaluation != null) {
                Map<String, Object> evalMap = new HashMap<>();
                evalMap.put("score", parseJsonSafely(evaluation.getScoreJson()));
                evalMap.put("strengths", parseJsonSafely(evaluation.getStrengthsJson()));
                evalMap.put("weaknesses", parseJsonSafely(evaluation.getWeaknessesJson()));
                evalMap.put("suggestions", parseJsonSafely(evaluation.getSuggestionsJson()));
                qa.put("evaluation", evalMap);
            }

            qaList.add(qa);
        }
        return qaList;
    }

    // ── 响应构建 ──

    private InterviewStatusResponse buildStatusResponse(InterviewSession session,
                                                         InterviewQuestionDTO currentQuestion) {
        boolean completed = isTerminalStatus(session.getStatus());
        List<InterviewStatusResponse.PerQuestionScore> scores = buildPerQuestionScores(session.getId());
        return new InterviewStatusResponse(
                session.getId(),
                session.getStatus(),
                session.getCurrentQuestionIndex() != null ? session.getCurrentQuestionIndex() : 0,
                session.getTotalQuestions() != null ? session.getTotalQuestions() : DEFAULT_TOTAL_QUESTIONS,
                currentQuestion,
                session.getSummaryJson(),
                completed,
                scores,
                session.getPersonaName(),
                session.getPersonaTitle()
        );
    }

    private List<InterviewStatusResponse.PerQuestionScore> buildPerQuestionScores(Long sessionId) {
        QueryWrapper<InterviewEvaluation> query = new QueryWrapper<>();
        query.eq("session_id", sessionId).orderByAsc("id");
        List<InterviewEvaluation> evaluations = evaluationMapper.selectList(query);
        List<InterviewStatusResponse.PerQuestionScore> scores = new ArrayList<>();
        for (InterviewEvaluation eval : evaluations) {
            InterviewQuestion question = questionMapper.selectById(eval.getQuestionId());
            if (question == null) continue;
            Map<String, Integer> scoreMap = parseScoreJson(eval.getScoreJson());
            scores.add(new InterviewStatusResponse.PerQuestionScore(
                    question.getQuestionIndex(),
                    question.getQuestionText() != null ? question.getQuestionText() : "",
                    scoreMap.getOrDefault("clarity", 0),
                    scoreMap.getOrDefault("relevance", 0),
                    scoreMap.getOrDefault("depth", 0),
                    scoreMap.getOrDefault("structure", 0),
                    scoreMap.containsKey("evidence")
                            ? scoreMap.get("evidence")
                            : scoreMap.getOrDefault("accuracy", 0),
                    scoreMap.getOrDefault("accuracy", 0)
            ));
        }
        return scores;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Integer> parseScoreJson(String scoreJson) {
        if (scoreJson == null) return Map.of();
        try {
            Map<String, Object> raw = objectMapper.readValue(scoreJson,
                    new TypeReference<Map<String, Object>>() {});
            Map<String, Integer> normalized = new HashMap<>();
            for (Map.Entry<String, Object> entry : raw.entrySet()) {
                Integer value = normalizeScoreValue(entry.getValue());
                if (value != null) normalized.put(entry.getKey(), value);
            }
            return normalized;
        } catch (Exception e) {
            log.warn("scoreJson 解析失败", e);
            return Map.of();
        }
    }

    private SubmitAnswerResponse buildSubmitResponse(InterviewSession session,
                                                      InterviewQuestion nextQuestion,
                                                      InterviewEvaluation evaluation,
                                                      boolean completed,
                                                      boolean retryable) {
        return buildSubmitResponse(session, nextQuestion, evaluation, completed, retryable, null, null);
    }

    private SubmitAnswerResponse buildSubmitResponse(InterviewSession session,
                                                      InterviewQuestion nextQuestion,
                                                      InterviewEvaluation evaluation,
                                                      boolean completed,
                                                      boolean retryable,
                                                      AiErrorCategory errorCategory,
                                                      String errorMessage) {
        InterviewQuestionDTO nextQuestionDTO = nextQuestion != null
                ? toQuestionDTO(nextQuestion) : null;

        SubmitAnswerResponse.EvaluationSummary evalSummary = buildEvaluationSummary(evaluation);

        return new SubmitAnswerResponse(
                session.getId(),
                session.getStatus(),
                session.getCurrentQuestionIndex() != null ? session.getCurrentQuestionIndex() : 0,
                session.getTotalQuestions() != null ? session.getTotalQuestions() : DEFAULT_TOTAL_QUESTIONS,
                nextQuestionDTO,
                evalSummary,
                completed,
                retryable,
                errorCategory == null ? null : errorCategory.name(),
                errorMessage
        );
    }

    private SubmitAnswerResponse.EvaluationSummary buildEvaluationSummary(
            InterviewEvaluation evaluation) {
        if (evaluation == null) {
            return null;
        }
        Map<String, Integer> scoreMap = parseScoreJson(evaluation.getScoreJson());
        return new SubmitAnswerResponse.EvaluationSummary(
                parseJsonArray(evaluation.getStrengthsJson()),
                parseJsonArray(evaluation.getWeaknessesJson()),
                parseJsonArray(evaluation.getSuggestionsJson()),
                evaluation.getReferenceAnswerJson(),
                new SubmitAnswerResponse.EvaluationSummary.ScoreDetail(
                        scoreMap.getOrDefault("clarity", 0),
                        scoreMap.getOrDefault("relevance", 0),
                        scoreMap.getOrDefault("depth", 0),
                        scoreMap.getOrDefault("structure", 0),
                        scoreMap.containsKey("evidence")
                                ? scoreMap.get("evidence")
                                : scoreMap.getOrDefault("accuracy", 0),
                        scoreMap.getOrDefault("accuracy", 0)
                )
        );
    }

    private List<String> parseJsonArray(String json) {
        if (json == null) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("JSON 数组解析失败，返回空列表", e);
            return List.of();
        }
    }

    private InterviewQuestionDTO toQuestionDTO(InterviewQuestion question) {
        if (question == null) {
            return null;
        }
        return new InterviewQuestionDTO(
                question.getQuestionIndex() != null ? question.getQuestionIndex() : 0,
                question.getQuestionText(),
                question.getQuestionType(),
                question.getSource(),
                question.getSourceReference(),
                question.getProvenanceLabel()
        );
    }

    // ── 工具方法 ──

    private AiInvocation findInvocationByRequestId(String requestId) {
        QueryWrapper<AiInvocation> query = new QueryWrapper<>();
        query.eq("request_id", requestId);
        return aiInvocationMapper.selectOne(query);
    }

    private boolean isTerminalStatus(String status) {
        return "COMPLETED".equals(status) || "FAILED".equals(status) || "CANCELLED".equals(status);
    }

    // ── 跨会话总结 ──

    /**
     * 生成跨会话综合总结。
     *
     * @param request 包含要总结的会话 ID 列表
     * @return 综合总结响应
     */
    public MultiSessionSummaryResponse generateMultiSessionSummary(MultiSessionSummaryRequest request) {
        List<Long> sessionIds = request.sessionIds();
        List<Map<String, Object>> sessionSummaries = new ArrayList<>();
        List<MultiSessionSummaryResponse.SessionBrief> sessionBriefs = new ArrayList<>();

        for (Long sessionId : sessionIds) {
            InterviewSession session = loadSession(sessionId);

            // 校验会话已完成
            if (!isTerminalStatus(session.getStatus())) {
                throw new IllegalStateException(
                        "会话 " + sessionId + " 尚未完成，无法参与跨会话总结");
            }

            // 收集该会话的问答
            List<Map<String, Object>> qaList = buildQaList(sessionId);

            Map<String, Object> sessionSummary = new HashMap<>();
            sessionSummary.put("personaName", session.getPersonaName() != null ? session.getPersonaName() : "面试官");
            sessionSummary.put("personaTitle", session.getPersonaTitle() != null ? session.getPersonaTitle() : "");
            sessionSummary.put("qaList", qaList);
            sessionSummary.put("companyProfile", loadCompanyProfile(session.getJobDescriptionId()));
            sessionSummaries.add(sessionSummary);

            sessionBriefs.add(new MultiSessionSummaryResponse.SessionBrief(
                    session.getId(),
                    session.getPersonaName(),
                    session.getPersonaTitle(),
                    session.getTotalQuestions() != null ? session.getTotalQuestions() : 0
            ));
        }

        // 构建 prompt 并调用 AI
        String systemPrompt = promptBuilder.buildMultiSessionSummarySystemPrompt();
        String userMessage = promptBuilder.buildMultiSessionSummaryUserMessage(
                sessionSummaries, firstAvailableCompanyProfile(sessionSummaries));

        AiResult aiResult = invokeAiWithRetry("multi_session_summary", systemPrompt, userMessage, null);

        if (!aiResult.success()) {
            throw new IllegalStateException("AI 跨会话总结生成失败，请稍后重试");
        }

        Map<String, Object> parsed = parseAndValidateMultiSessionSummaryOutput(aiResult);
        if (parsed == null) {
            throw new IllegalStateException("AI 跨会话总结输出校验失败，请稍后重试");
        }

        @SuppressWarnings("unchecked")
        List<String> crossStrengths = (List<String>) parsed.getOrDefault("crossStrengths", List.of());
        @SuppressWarnings("unchecked")
        List<String> crossWeaknesses = (List<String>) parsed.getOrDefault("crossWeaknesses", List.of());
        @SuppressWarnings("unchecked")
        List<String> suggestions = (List<String>) parsed.getOrDefault("suggestions", List.of());

        return new MultiSessionSummaryResponse(
                parsed.getOrDefault("overallSummary", "").toString(),
                ((Number) parsed.getOrDefault("overallScore", 0)).intValue(),
                crossStrengths,
                crossWeaknesses,
                suggestions,
                sessionBriefs
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseAndValidateMultiSessionSummaryOutput(AiResult aiResult) {
        try {
            String json = outputValidator.extractJson(aiResult.content());
            var jsonResult = outputValidator.validateJson(json);
            if (!jsonResult.isValid()) {
                log.warn("跨会话总结 AI 输出 JSON 格式非法: requestId={}", aiResult.requestId());
                return null;
            }

            var fieldResult = outputValidator.validateRequiredFields(json,
                    List.of("overallScore", "overallSummary", "crossStrengths", "crossWeaknesses", "suggestions"));
            if (!fieldResult.isValid()) {
                log.warn("跨会话总结 AI 输出缺少必填字段: requestId={}, errors={}",
                        aiResult.requestId(), fieldResult.errors());
                return null;
            }

            // 校验数组字段
            for (String arrayField : new String[]{"crossStrengths", "crossWeaknesses", "suggestions"}) {
                var arrayResult = outputValidator.validateFieldType(json, arrayField, "array");
                if (!arrayResult.isValid()) {
                    log.warn("跨会话总结 AI 输出 {} 字段类型错误（非数组）: requestId={}", arrayField, aiResult.requestId());
                    return null;
                }
            }

            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            log.warn("跨会话总结 AI 输出解析失败: requestId={}", aiResult.requestId(), e);
            return null;
        }
    }

    private String toJsonString(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            log.error("JSON 序列化失败", e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Object parseJsonSafely(String json) {
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception e) {
            return json;
        }
    }

    /**
     * 完成计划后的规范化反馈投影入口：只映射持久化总结中的核心问题与建议，
     * 生成 PENDING 事件；不修改简历、Pipeline、知识或 Workspace 状态。
     * 持久化与消费归 Workspace Action（W1）。
     */
    public InterviewFeedbackEvent projectFeedbackEvent(Long planId, MultiSessionSummaryResponse summary) {
        if (feedbackProjector == null) {
            throw new IllegalStateException("反馈投影器不可用");
        }
        InterviewPlan plan = planMapper.selectById(planId);
        if (plan == null || plan.getUserId() == null
                || !Objects.equals(plan.getUserId(), CurrentUser.DEMO_USER_ID)) {
            throw new IllegalArgumentException("面试计划不存在");
        }
        return feedbackProjector.project(plan, summary);
    }
}
