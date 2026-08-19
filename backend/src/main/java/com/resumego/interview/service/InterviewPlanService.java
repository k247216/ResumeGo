package com.resumego.interview.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumego.common.CurrentUser;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 多轮模拟面试计划服务。
 * <p>
 * 只负责“一次面试”和“多位面试官轮次”的持久化归属，
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
    private final ObjectMapper objectMapper;

    @Transactional
    public InterviewPlanResponse createPlan(CreateInterviewPlanRequest request) {
        List<Long> personaIds = request.personaIds();
        if (personaIds == null || personaIds.isEmpty()) {
            throw new IllegalArgumentException("至少选择一位面试官");
        }
        if (personaIds.size() > 5) {
            throw new IllegalArgumentException("面试官数量最多为 5 位");
        }

        List<InterviewerPersona> personas = new ArrayList<>();
        for (Long personaId : personaIds) {
            InterviewerPersona persona = personaMapper.selectById(personaId);
            if (persona == null) {
                throw new IllegalArgumentException("面试官人设不存在: " + personaId);
            }
            personas.add(persona);
        }

        LocalDateTime now = LocalDateTime.now();
        InterviewPlan plan = new InterviewPlan();
        plan.setUserId(CurrentUser.DEMO_USER_ID);
        plan.setResumeVersionId(request.resumeVersionId());
        plan.setJobDescriptionId(request.jobDescriptionId());
        plan.setTitle("多轮模拟面试");
        plan.setQuestionCount(request.questionCount());
        plan.setPersonaPlanJson(writeJson(personas.stream()
                .map(persona -> Map.of(
                        "personaId", persona.getId(),
                        "personaName", persona.getName(),
                        "personaTitle", persona.getTitle()
                ))
                .toList()));
        plan.setFocusTagsJson(writeJson(request.focusTags() != null ? request.focusTags() : List.of()));
        plan.setSupplementText(request.supplement());
        plan.setCreatedAt(now);
        plan.setUpdatedAt(now);
        planMapper.insert(plan);

        List<InterviewPlanResponse.Round> rounds = new ArrayList<>();
        for (int index = 0; index < personas.size(); index++) {
            InterviewerPersona persona = personas.get(index);
            int roundOrder = index + 1;
            InterviewStatusResponse created = interviewService.createInterview(new StartInterviewRequest(
                    request.resumeVersionId(),
                    request.jobDescriptionId(),
                    request.questionCount(),
                    persona.getId()
            ));
            bindSessionToPlan(created.sessionId(), plan.getId(), roundOrder);
            rounds.add(toRound(created, persona.getId(), roundOrder));
        }

        return new InterviewPlanResponse(
                plan.getId(),
                plan.getResumeVersionId(),
                plan.getJobDescriptionId(),
                plan.getTitle(),
                plan.getQuestionCount(),
                parseStringList(plan.getFocusTagsJson()),
                plan.getSupplementText(),
                null,
                null,
                rounds,
                rounds.stream().allMatch(InterviewPlanResponse.Round::completed),
                plan.getCreatedAt(),
                plan.getUpdatedAt()
        );
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

        return new InterviewPlanResponse(
                plan.getId(),
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
