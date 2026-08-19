package com.resumego.interview.controller;

import com.resumego.interview.dto.CreateInterviewPlanRequest;
import com.resumego.interview.dto.InterviewPlanResponse;
import com.resumego.interview.dto.MultiSessionSummaryResponse;
import com.resumego.interview.service.InterviewPlanService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 多轮面试计划的 HTTP 协议测试，不涉及任一轮状态转换。 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InterviewPlanController 协议映射测试")
class InterviewPlanControllerTest {
    @Mock private InterviewPlanService planService;

    @Test
    void shouldCreateOrRejectPlan() {
        when(planService.createPlan(any())).thenReturn(plan());
        assertThat(controller().createPlan(request()).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        reset(planService);
        when(planService.createPlan(any())).thenThrow(new IllegalArgumentException("无效"));
        assertThat(controller().createPlan(request()).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldListAndGetPlan() {
        when(planService.listMyPlans()).thenReturn(List.of(plan()));
        when(planService.getPlan(1L)).thenReturn(plan());
        assertThat(controller().listMyPlans().getBody().data()).hasSize(1);
        assertThat(controller().getPlan(1L).getStatusCode()).isEqualTo(HttpStatus.OK);
        reset(planService);
        when(planService.getPlan(1L)).thenThrow(new IllegalArgumentException("不存在"));
        assertThat(controller().getPlan(1L).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldMapPlanSummaryResponses() {
        when(planService.generatePlanSummary(1L)).thenReturn(summary());
        assertThat(controller().generatePlanSummary(1L).getStatusCode()).isEqualTo(HttpStatus.OK);
        reset(planService);
        when(planService.generatePlanSummary(1L)).thenThrow(new IllegalArgumentException("不存在"));
        assertThat(controller().generatePlanSummary(1L).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        reset(planService);
        when(planService.generatePlanSummary(1L)).thenThrow(new IllegalStateException("未完成"));
        assertThat(controller().generatePlanSummary(1L).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldHideOrRejectPlan() {
        assertThat(controller().hidePlan(1L).getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(planService).hidePlan(1L);
        reset(planService);
        doThrow(new IllegalArgumentException("不存在")).when(planService).hidePlan(1L);
        assertThat(controller().hidePlan(1L).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    private InterviewPlanController controller() { return new InterviewPlanController(planService); }
    private CreateInterviewPlanRequest request() { return new CreateInterviewPlanRequest(10L, 20L, 5, List.of(1L), List.of(), null); }
    private InterviewPlanResponse plan() { return new InterviewPlanResponse(1L, 10L, 20L, "计划", 5, List.of(), null, null, null, List.of(), false, null, null); }
    private MultiSessionSummaryResponse summary() { return new MultiSessionSummaryResponse("总结", 80, List.of(), List.of(), List.of(), List.of()); }
}
