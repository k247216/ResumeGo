package com.resumego.interview.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumego.interview.dto.CreateInterviewPlanRequest;
import com.resumego.interview.dto.InterviewPlanResponse;
import com.resumego.interview.dto.MultiSessionSummaryResponse;
import com.resumego.interview.dto.InterviewStatusResponse;
import com.resumego.interview.dto.StartInterviewRequest;
import com.resumego.interview.entity.InterviewPlan;
import com.resumego.interview.entity.InterviewSession;
import com.resumego.interview.entity.InterviewerPersona;
import com.resumego.interview.mapper.InterviewPlanMapper;
import com.resumego.interview.mapper.InterviewSessionMapper;
import com.resumego.interview.mapper.InterviewerPersonaMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("InterviewPlanService 单元测试")
class InterviewPlanServiceTest {

    @Mock
    private InterviewPlanMapper planMapper;
    @Mock
    private InterviewSessionMapper sessionMapper;
    @Mock
    private InterviewerPersonaMapper personaMapper;
    @Mock
    private InterviewService interviewService;
    @Mock
    private InterviewGrowthService growthService;

    private InterviewPlanService planService;

    @BeforeEach
    void setUp() {
        planService = new InterviewPlanService(
                planMapper,
                sessionMapper,
                personaMapper,
                interviewService,
                growthService,
                new ObjectMapper()
        );
    }

    @Test
    @DisplayName("创建面试计划时按最初选择的人设顺序绑定每一轮会话")
    void shouldCreatePlanAndBindSessionsInPersonaOrder() {
        when(personaMapper.selectById(1L)).thenReturn(buildPersona(1L, "技术面试官", "高级后端工程师"));
        when(personaMapper.selectById(2L)).thenReturn(buildPersona(2L, "HR 面试官", "招聘经理"));
        doAnswer(invocation -> {
            InterviewPlan plan = invocation.getArgument(0);
            plan.setId(100L);
            return 1;
        }).when(planMapper).insert(any(InterviewPlan.class));
        when(interviewService.createInterview(new StartInterviewRequest(10L, 20L, 5, 1L)))
                .thenReturn(buildStatus(501L, "技术面试官", "高级后端工程师"));
        when(interviewService.createInterview(new StartInterviewRequest(10L, 20L, 5, 2L)))
                .thenReturn(buildStatus(502L, "HR 面试官", "招聘经理"));

        InterviewPlanResponse response = planService.createPlan(new CreateInterviewPlanRequest(
                10L,
                20L,
                5,
                List.of(1L, 2L),
                List.of("项目深挖"),
                "重点考察分布式项目"
        ));

        assertThat(response.planId()).isEqualTo(100L);
        assertThat(response.summary()).isNull();
        assertThat(response.rounds()).extracting(InterviewPlanResponse.Round::sessionId)
                .containsExactly(501L, 502L);
        assertThat(response.rounds()).extracting(InterviewPlanResponse.Round::roundOrder)
                .containsExactly(1, 2);
        assertThat(response.rounds()).extracting(InterviewPlanResponse.Round::personaName)
                .containsExactly("技术面试官", "HR 面试官");

        verify(sessionMapper).updateById(argThat((InterviewSession session) ->
                session.getId().equals(501L)
                        && session.getPlanId().equals(100L)
                        && session.getRoundOrder().equals(1)
        ));
        verify(sessionMapper).updateById(argThat((InterviewSession session) ->
                session.getId().equals(502L)
                        && session.getPlanId().equals(100L)
                        && session.getRoundOrder().equals(2)
        ));
    }

    @Test
    @DisplayName("生成整次多轮总结时按计划轮次收集已完成会话并保存到计划")
    void shouldGenerateAndPersistPlanSummary() {
        InterviewPlan plan = buildPlan(100L, null);
        when(planMapper.selectById(100L)).thenReturn(plan);
        when(sessionMapper.selectList(any())).thenReturn(List.of(
                buildPlanSession(501L, 100L, 1, "COMPLETED"),
                buildPlanSession(502L, 100L, 2, "COMPLETED")
        ));
        MultiSessionSummaryResponse summary = new MultiSessionSummaryResponse(
                "两轮面试整体表现稳定，但项目量化表达还可以加强。",
                82,
                List.of("技术基础扎实"),
                List.of("项目结果表达偏弱"),
                List.of("补充项目指标和复盘"),
                List.of(
                        new MultiSessionSummaryResponse.SessionBrief(501L, "技术面试官", "高级后端工程师", 5),
                        new MultiSessionSummaryResponse.SessionBrief(502L, "HR 面试官", "招聘经理", 5)
                )
        );
        when(interviewService.generateMultiSessionSummary(argThat(request ->
                request.sessionIds().equals(List.of(501L, 502L))
        ))).thenReturn(summary);

        MultiSessionSummaryResponse response = planService.generatePlanSummary(100L);

        assertThat(response.overallScore()).isEqualTo(82);
        verify(planMapper).updateById(argThat((InterviewPlan saved) ->
                saved.getId().equals(100L)
                        && saved.getSummaryJson() != null
                        && saved.getSummaryJson().contains("两轮面试整体表现稳定")
                        && saved.getSummaryGeneratedAt() != null
        ));
        verify(growthService).upsertSnapshotForPlan(eq(plan), any(), eq(summary));
    }

    @Test
    @DisplayName("计划存在未完成轮次时不能生成整次总结")
    void shouldRejectPlanSummaryWhenAnyRoundUnfinished() {
        InterviewPlan plan = buildPlan(100L, null);
        when(planMapper.selectById(100L)).thenReturn(plan);
        when(sessionMapper.selectList(any())).thenReturn(List.of(
                buildPlanSession(501L, 100L, 1, "COMPLETED"),
                buildPlanSession(502L, 100L, 2, "WAITING_ANSWER")
        ));

        assertThatThrownBy(() -> planService.generatePlanSummary(100L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("尚未完成");

        verify(interviewService, never()).generateMultiSessionSummary(any());
    }

    @Test
    @DisplayName("计划存在失败或取消轮次时不能当作完整面试生成整次总结")
    void shouldRejectPlanSummaryWhenAnyRoundFailedOrCancelled() {
        InterviewPlan plan = buildPlan(100L, null);
        when(planMapper.selectById(100L)).thenReturn(plan);
        when(sessionMapper.selectList(any())).thenReturn(List.of(
                buildPlanSession(501L, 100L, 1, "COMPLETED"),
                buildPlanSession(502L, 100L, 2, "FAILED"),
                buildPlanSession(503L, 100L, 3, "CANCELLED")
        ));

        assertThatThrownBy(() -> planService.generatePlanSummary(100L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("尚未完成");

        verify(interviewService, never()).generateMultiSessionSummary(any());
    }

    @Test
    @DisplayName("计划响应中只有 COMPLETED 轮次应标记为已完成")
    void shouldOnlyMarkCompletedRoundsAsCompleted() {
        InterviewPlan plan = buildPlan(100L, null);
        when(planMapper.selectById(100L)).thenReturn(plan);
        when(sessionMapper.selectList(any())).thenReturn(List.of(
                buildPlanSession(501L, 100L, 1, "COMPLETED"),
                buildPlanSession(502L, 100L, 2, "FAILED"),
                buildPlanSession(503L, 100L, 3, "CANCELLED")
        ));

        InterviewPlanResponse response = planService.getPlan(100L);

        assertThat(response.completed()).isFalse();
        assertThat(response.rounds()).extracting(InterviewPlanResponse.Round::completed)
                .containsExactly(true, false, false);
    }

    @Test
    @DisplayName("计划响应应反序列化已保存的整次总结")
    void shouldExposePersistedSummaryInPlanResponse() {
        InterviewPlan plan = buildPlan(100L, """
                {
                  "overallSummary": "整次复盘已保存",
                  "overallScore": 88,
                  "crossStrengths": ["表达结构稳定"],
                  "crossWeaknesses": ["缺少指标"],
                  "suggestions": ["补充量化数据"],
                  "sessions": []
                }
                """);
        when(planMapper.selectById(100L)).thenReturn(plan);
        when(sessionMapper.selectList(any())).thenReturn(List.of(
                buildPlanSession(501L, 100L, 1, "COMPLETED")
        ));

        InterviewPlanResponse response = planService.getPlan(100L);

        assertThat(response.summary()).isNotNull();
        assertThat(response.summary().overallScore()).isEqualTo(88);
        assertThat(response.summary().overallSummary()).isEqualTo("整次复盘已保存");
    }

    private InterviewerPersona buildPersona(Long id, String name, String title) {
        InterviewerPersona persona = new InterviewerPersona();
        persona.setId(id);
        persona.setName(name);
        persona.setTitle(title);
        persona.setStyle("追问项目细节");
        persona.setAvatar("default");
        persona.setType("preset");
        return persona;
    }

    private InterviewPlan buildPlan(Long id, String summaryJson) {
        InterviewPlan plan = new InterviewPlan();
        plan.setId(id);
        plan.setUserId(1L);
        plan.setResumeVersionId(10L);
        plan.setJobDescriptionId(20L);
        plan.setTitle("多轮模拟面试");
        plan.setQuestionCount(5);
        plan.setPersonaPlanJson("[]");
        plan.setFocusTagsJson("[]");
        plan.setSummaryJson(summaryJson);
        return plan;
    }

    private InterviewSession buildPlanSession(Long id, Long planId, int roundOrder, String status) {
        InterviewSession session = new InterviewSession();
        session.setId(id);
        session.setPlanId(planId);
        session.setRoundOrder(roundOrder);
        session.setPersonaId((long) roundOrder);
        session.setPersonaName(roundOrder == 1 ? "技术面试官" : "HR 面试官");
        session.setPersonaTitle(roundOrder == 1 ? "高级后端工程师" : "招聘经理");
        session.setStatus(status);
        session.setCurrentQuestionIndex("COMPLETED".equals(status) ? 5 : 2);
        session.setTotalQuestions(5);
        return session;
    }

    private InterviewStatusResponse buildStatus(Long sessionId, String personaName, String personaTitle) {
        return new InterviewStatusResponse(
                sessionId,
                "READY",
                0,
                5,
                null,
                null,
                false,
                List.of(),
                personaName,
                personaTitle
        );
    }
}
