package com.resumego.interview.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumego.common.CurrentUser;
import com.resumego.interview.InterviewMode;
import com.resumego.interview.context.InterviewContextSnapshot;
import com.resumego.interview.context.InterviewContextValidator;
import com.resumego.interview.context.InterviewStartContext;
import com.resumego.interview.dto.CreateInterviewPlanRequest;
import com.resumego.interview.dto.InterviewPlanResponse;
import com.resumego.interview.dto.InterviewStatusResponse;
import com.resumego.interview.dto.MultiSessionSummaryRequest;
import com.resumego.interview.dto.MultiSessionSummaryResponse;
import com.resumego.interview.dto.StartInterviewRequest;
import com.resumego.interview.entity.InterviewPlan;
import com.resumego.interview.entity.InterviewSession;
import com.resumego.interview.entity.InterviewerPersona;
import com.resumego.interview.mapper.InterviewPlanMapper;
import com.resumego.interview.mapper.InterviewSessionMapper;
import com.resumego.interview.mapper.InterviewerPersonaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 多轮模拟面试计划服务。
 * <p>
 * 只负责“一次面试”和“多位面试官轮次”的持久化归属与模式上下文快照，
 * 不实现、不修改每轮面试的状态机转换。
 */
@Service
@RequiredArgsConstructor
public class InterviewPlanService {

    private final InterviewPlanMapper planMapper;
    private final InterviewSessionMapper sessionMapper;
    private final InterviewerPersonaMapper personaMapper;
    private final InterviewService interviewService;
    private final InterviewGrowthService growthService;
    private final List<InterviewContextValidator> contextValidators;
    private final ObjectMapper objectMapper;

    @Transactional
    public InterviewPlanResponse createPlan(CreateInterviewPlanRequest request) {
        InterviewStartContext context = request.toContext();
        InterviewContextSnapshot snapshot = validateContext(context);

        LocalDateTime now = LocalDateTime.now();
        InterviewPlan plan = new InterviewPlan();
        plan.setUserId(CurrentUser.DEMO_USER_ID);
        plan.setMode(context.mode().name());
        plan.setContextContractVersion(snapshot.contextContractVersion());
        plan.setStartContextSnapshotJson(writeJson(snapshot));
        plan.setResumeVersionId(snapshot.resumeVersionId());
        plan.setJobDescriptionId(snapshot.jobDescriptionId());
        plan.setTitle("多轮模拟面试");
        plan.setQuestionCount(request.questionCount() != null ? request.questionCount() : 5);
        plan.setPersonaPlanJson(writeJson(personaPlan(context.mode(), snapshot)));
        plan.setFocusTagsJson(writeJson(request.focusTags() != null ? request.focusTags() : List.of()));
        plan.setSupplementText(request.supplement());
        plan.setCreatedAt(now);
        plan.setUpdatedAt(now);
        planMapper.insert(plan);

        List<InterviewPlanResponse.Round> rounds = new ArrayList<>();
        if (context.mode() == InterviewMode.ROLE_BASED) {
            List<Long> personaIds = snapshot.personaIds();
            for (int index = 0; index < personaIds.size(); index++) {
                Long personaId = personaIds.get(index);
                int roundOrder = index + 1;
                InterviewStatusResponse created = interviewService.createInterview(new StartInterviewRequest(
                        snapshot.resumeVersionId(),
                        snapshot.jobDescriptionId(),
                        plan.getQuestionCount(),
                        personaId
                ));
                bindSessionToPlan(created.sessionId(), plan.getId(), roundOrder);
                rounds.add(toRound(created, personaId, roundOrder));
            }
        }
        // 知识训练与面经模式的轮次创建在问题来源适配器就绪后进行（Task 3），计划先落库并携带快照。

        return buildResponse(plan, rounds);
    }

    private Map<String, Object> personaPlan(InterviewMode mode, InterviewContextSnapshot snapshot) {
        // 快照已包含 persona 顺序；persona_plan_json 保持既有结构以兼容现有读取方
        List<Map<String, Object>> plan = new ArrayList<>();
        if (snapshot.personaIds() != null) {
            for (int index = 0; index < snapshot.personaIds().size(); index++) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("personaId", snapshot.personaIds().get(index));
                if (snapshot.personaNames() != null && index < snapshot.personaNames().size()) {
                    row.put("personaName", snapshot.personaNames().get(index));
                }
                plan.add(row);
            }
        }
        return Map.of("personas", plan);
    }

    private InterviewContextSnapshot validateContext(InterviewStartContext context) {
        return contextValidators.stream()
                .filter(validator -> validator.supports(context.mode()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("没有支持该模式的校验器: " + context.mode()))
                .validate(context);
    }

    public List<InterviewPlanResponse> listMyPlans() {
        QueryWrapper<InterviewPlan> query = new QueryWrapper<>();
        query.eq("user_id", CurrentUser.DEMO_USER_ID)
                .isNull("deleted_at")
                .orderByDesc("created_at");
        List<InterviewPlan> plans = planMapper.selectList(query);

        List<InterviewPlanResponse> result = new ArrayList<>();
        for (InterviewPlan plan : plans) {
            result.add(toResponse(plan));
        }
        return result;
    }

    public InterviewPlanResponse getPlan(Long planId) {
        InterviewPlan plan = loadOwnedPlan(planId);
        if (plan.getDeletedAt() != null) {
            throw new IllegalArgumentException("面试计划不存在");
        }
        return toResponse(plan);
    }

    @Transactional
    public MultiSessionSummaryResponse generatePlanSummary(Long planId) {
        InterviewPlan plan = loadOwnedPlan(planId);
        if (plan.getDeletedAt() != null) {
            throw new IllegalArgumentException("面试计划不存在");
        }

        List<InterviewSession> planSessions = loadPlanSessions(plan.getId());
        if (planSessions.size() < 2) {
            throw new IllegalStateException("至少需要完成 2 轮面试后才能生成整次总结");
        }
        List<Long> completedSessionIds = new ArrayList<>();
        for (InterviewSession session : planSessions) {
            if (!isCompletedStatus(session.getStatus())) {
                throw new IllegalStateException("本次面试尚未完成，无法生成整次总结");
            }
            completedSessionIds.add(session.getId());
        }

        MultiSessionSummaryResponse summary = interviewService.generateMultiSessionSummary(
                new MultiSessionSummaryRequest(completedSessionIds)
        );

        plan.setSummaryJson(writeJson(summary));
        plan.setSummaryGeneratedAt(LocalDateTime.now());
        plan.setUpdatedAt(LocalDateTime.now());
        planMapper.updateById(plan);
        growthService.upsertSnapshotForPlan(plan, planSessions, summary);
        return summary;
    }

    @Transactional
    public void hidePlan(Long planId) {
        InterviewPlan plan = loadOwnedPlan(planId);
        plan.setDeletedAt(LocalDateTime.now());
        plan.setUpdatedAt(LocalDateTime.now());
        planMapper.updateById(plan);
    }

    private void bindSessionToPlan(Long sessionId, Long planId, Integer roundOrder) {
        InterviewSession update = new InterviewSession();
        update.setId(sessionId);
        update.setPlanId(planId);
        update.setRoundOrder(roundOrder);
        sessionMapper.updateById(update);
    }

    private InterviewPlanResponse toResponse(InterviewPlan plan) {
        List<InterviewSession> sessions = loadPlanSessions(plan.getId());

        List<InterviewPlanResponse.Round> rounds = sessions.stream()
                .map(session -> new InterviewPlanResponse.Round(
                        session.getId(),
                        session.getPersonaId(),
                        session.getPersonaName(),
                        session.getPersonaTitle(),
                        session.getRoundOrder(),
                        session.getStatus(),
                        session.getCurrentQuestionIndex() != null ? session.getCurrentQuestionIndex() : 0,
                        session.getTotalQuestions() != null ? session.getTotalQuestions() : plan.getQuestionCount(),
                        isCompletedStatus(session.getStatus())
                ))
                .toList();

        return buildResponse(plan, rounds);
    }

    /** 响应中的快照来自持久化 JSON：源对象改名不改变历史展示。 */
    private InterviewPlanResponse buildResponse(InterviewPlan plan, List<InterviewPlanResponse.Round> rounds) {
        return new InterviewPlanResponse(
                plan.getId(),
                plan.getMode(),
                plan.getContextContractVersion(),
                parseSnapshot(plan.getStartContextSnapshotJson()),
                plan.getResumeVersionId(),
                plan.getJobDescriptionId(),
                plan.getTitle(),
                plan.getQuestionCount(),
                parseStringList(plan.getFocusTagsJson()),
                plan.getSupplementText(),
                parseSummary(plan.getSummaryJson()),
                plan.getSummaryGeneratedAt(),
                rounds,
                !rounds.isEmpty() && rounds.stream().allMatch(InterviewPlanResponse.Round::completed),
                plan.getCreatedAt(),
                plan.getUpdatedAt()
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseSnapshot(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, LinkedHashMap.class);
        } catch (Exception e) {
            return Map.of();
        }
    }

    private InterviewPlan loadOwnedPlan(Long planId) {
        InterviewPlan plan = planMapper.selectById(planId);
        if (plan == null || plan.getUserId() == null
                || !Objects.equals(plan.getUserId(), CurrentUser.DEMO_USER_ID)) {
            throw new IllegalArgumentException("面试计划不存在");
        }
        return plan;
    }

    private List<InterviewSession> loadPlanSessions(Long planId) {
        QueryWrapper<InterviewSession> sessionQuery = new QueryWrapper<>();
        sessionQuery.eq("plan_id", planId).orderByAsc("round_order");
        return sessionMapper.selectList(sessionQuery);
    }

    private MultiSessionSummaryResponse parseSummary(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, MultiSessionSummaryResponse.class);
        } catch (Exception e) {
            return null;
        }
    }

    private InterviewPlanResponse.Round toRound(InterviewStatusResponse status, Long personaId, int roundOrder) {
        return new InterviewPlanResponse.Round(
                status.sessionId(),
                personaId,
                status.personaName(),
                status.personaTitle(),
                roundOrder,
                status.status(),
                status.currentQuestionIndex(),
                status.totalQuestions(),
                status.completed()
        );
    }

    private boolean isCompletedStatus(String status) {
        return "COMPLETED".equals(status);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException("面试计划 JSON 序列化失败", e);
        }
    }

    private List<String> parseStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }
}
