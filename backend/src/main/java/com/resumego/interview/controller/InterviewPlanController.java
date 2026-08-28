package com.resumego.interview.controller;

import com.resumego.common.ApiResponse;
import com.resumego.interview.dto.CreateInterviewPlanRequest;
import com.resumego.interview.dto.InterviewPlanResponse;
import com.resumego.interview.dto.MultiSessionSummaryResponse;
import com.resumego.interview.service.InterviewPlanService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 多轮面试计划 Controller。
 * 计划只负责把多个单轮面试会话组织为“一次面试”，不控制状态机。
 */
@RestController
@Validated
@RequiredArgsConstructor
public class InterviewPlanController {

    private final InterviewPlanService planService;

    @PostMapping("/api/v1/interview-plans")
    public ResponseEntity<ApiResponse<InterviewPlanResponse>> createPlan(
            @Valid @RequestBody CreateInterviewPlanRequest request) {
        try {
            InterviewPlanResponse response = planService.createPlan(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.fail("请求参数无效，请检查输入"));
        } catch (IllegalStateException e) {
            // AI 未配置或题目来源不可用时，保留可行动的提示给前端配置引导。
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.fail(e.getMessage()));
        }
    }

    @GetMapping("/api/v1/interview-plans/my")
    public ResponseEntity<ApiResponse<List<InterviewPlanResponse>>> listMyPlans() {
        return ResponseEntity.ok(ApiResponse.ok(planService.listMyPlans()));
    }

    @GetMapping("/api/v1/interview-plans/{planId}")
    public ResponseEntity<ApiResponse<InterviewPlanResponse>> getPlan(
            @PathVariable @Positive(message = "planId 必须为正整数") Long planId) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(planService.getPlan(planId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.fail("面试计划不存在"));
        }
    }

    @PostMapping("/api/v1/interview-plans/{planId}/summary")
    public ResponseEntity<ApiResponse<MultiSessionSummaryResponse>> generatePlanSummary(
            @PathVariable @Positive(message = "planId 必须为正整数") Long planId) {
        try {
            return ResponseEntity.ok(ApiResponse.ok(planService.generatePlanSummary(planId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.fail("面试计划不存在"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.fail(e.getMessage()));
        }
    }

    @DeleteMapping("/api/v1/interview-plans/{planId}")
    public ResponseEntity<ApiResponse<Void>> hidePlan(
            @PathVariable @Positive(message = "planId 必须为正整数") Long planId) {
        try {
            planService.hidePlan(planId);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.fail("面试计划不存在"));
        }
    }
}
