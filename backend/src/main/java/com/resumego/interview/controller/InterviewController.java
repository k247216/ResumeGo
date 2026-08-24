package com.resumego.interview.controller;

import com.resumego.interview.dto.InterviewStatusResponse;
import com.resumego.interview.dto.MultiSessionSummaryRequest;
import com.resumego.interview.dto.MultiSessionSummaryResponse;
import com.resumego.interview.dto.SessionHistoryResponse;
import com.resumego.interview.dto.StartInterviewRequest;
import com.resumego.interview.dto.SubmitAnswerRequest;
import com.resumego.interview.dto.SubmitAnswerResponse;
import com.resumego.interview.service.InterviewService;
import com.resumego.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 模拟面试 Controller。
 * 提供创建面试、开始面试、查询状态、提交回答的 REST 接口。
 * 状态机逻辑由 {@link InterviewService} 编排，Controller 仅做协议转换和参数校验。
 */
@RestController
@Validated
@RequiredArgsConstructor
public class InterviewController {

    private static final Logger log = LoggerFactory.getLogger(InterviewController.class);

    private final InterviewService interviewService;

    /**
     * 创建面试会话。
     * POST /api/v1/interviews
     */
    @PostMapping("/api/v1/interviews")
    public ResponseEntity<ApiResponse<InterviewStatusResponse>> createInterview(
            @Valid @RequestBody StartInterviewRequest request) {
        log.info("创建面试会话: resumeVersionId={}, jobDescriptionId={}",
                request.resumeVersionId(), request.jobDescriptionId());
        try {
            InterviewStatusResponse response = interviewService.createInterview(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.ok(response));
        } catch (IllegalArgumentException e) {
            log.warn("创建面试参数校验失败: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.fail("请求参数无效，请检查输入"));
        } catch (Exception e) {
            log.error("创建面试时发生未预期错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("服务内部错误，请稍后重试"));
        }
    }

    /**
     * 开始面试（触发 AI 出第一题）。
     * POST /api/v1/interviews/{sessionId}/start
     */
    @PostMapping("/api/v1/interviews/{sessionId}/start")
    public ResponseEntity<ApiResponse<InterviewStatusResponse>> startInterview(
            @PathVariable @Positive(message = "sessionId 必须为正整数") Long sessionId) {
        log.info("开始面试: sessionId={}", sessionId);
        try {
            InterviewStatusResponse response = interviewService.startInterview(sessionId);
            return ResponseEntity.ok(ApiResponse.ok(response));
        } catch (IllegalArgumentException e) {
            log.warn("开始面试参数校验失败: sessionId={}, message={}", sessionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.fail("会话不存在"));
        } catch (IllegalStateException e) {
            // 透传面向用户的提示（如 AI 未配置），状态错误保留通用文案
            log.warn("开始面试失败: sessionId={}, message={}", sessionId, e.getMessage());
            String message = e.getMessage() != null && e.getMessage().contains("AI 模型服务")
                    ? e.getMessage()
                    : "当前状态不允许开始面试";
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.fail(message));
        } catch (Exception e) {
            log.error("开始面试时发生未预期错误: sessionId={}", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("服务内部错误，请稍后重试"));
        }
    }

    /**
     * 查询面试状态。
     * GET /api/v1/interviews/{sessionId}/status
     */
    @GetMapping("/api/v1/interviews/{sessionId}/status")
    public ResponseEntity<ApiResponse<InterviewStatusResponse>> getInterviewStatus(
            @PathVariable @Positive(message = "sessionId 必须为正整数") Long sessionId) {
        log.info("查询面试状态: sessionId={}", sessionId);
        try {
            InterviewStatusResponse response = interviewService.getInterviewStatus(sessionId);
            return ResponseEntity.ok(ApiResponse.ok(response));
        } catch (IllegalArgumentException e) {
            log.warn("查询状态参数校验失败: sessionId={}, message={}", sessionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.fail("会话不存在"));
        } catch (Exception e) {
            log.error("查询面试状态时发生未预期错误: sessionId={}", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("服务内部错误，请稍后重试"));
        }
    }

    /**
     * 提交回答。
     * POST /api/v1/interviews/{sessionId}/answers
     */
    @PostMapping("/api/v1/interviews/{sessionId}/answers")
    public ResponseEntity<ApiResponse<SubmitAnswerResponse>> submitAnswer(
            @PathVariable @Positive(message = "sessionId 必须为正整数") Long sessionId,
            @Valid @RequestBody SubmitAnswerRequest request) {
        log.info("提交回答: sessionId={}", sessionId);
        try {
            SubmitAnswerResponse response = interviewService.submitAnswer(sessionId, request);
            return ResponseEntity.ok(ApiResponse.ok(response));
        } catch (IllegalArgumentException e) {
            log.warn("提交回答参数校验失败: sessionId={}, message={}", sessionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.fail("请求参数无效，请检查输入"));
        } catch (IllegalStateException e) {
            log.warn("提交回答状态校验失败: sessionId={}, message={}", sessionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.fail("当前状态不允许提交回答"));
        } catch (Exception e) {
            log.error("提交回答时发生未预期错误: sessionId={}", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("服务内部错误，请稍后重试"));
        }
    }

    /**
     * 列出当前用户的所有面试会话。
     * GET /api/v1/interviews/my
     */
    @GetMapping("/api/v1/interviews/my")
    public ResponseEntity<ApiResponse<List<InterviewStatusResponse>>> listMyInterviews() {
        log.info("查询用户面试会话列表");
        try {
            List<InterviewStatusResponse> response = interviewService.listMyInterviews();
            return ResponseEntity.ok(ApiResponse.ok(response));
        } catch (Exception e) {
            log.error("查询用户面试会话列表时发生未预期错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("服务内部错误，请稍后重试"));
        }
    }

    /**
     * 获取会话完整问答历史。
     * GET /api/v1/interviews/{sessionId}/history
     */
    @GetMapping("/api/v1/interviews/{sessionId}/history")
    public ResponseEntity<ApiResponse<SessionHistoryResponse>> getSessionHistory(
            @PathVariable @Positive(message = "sessionId 必须为正整数") Long sessionId) {
        log.info("查询会话历史: sessionId={}", sessionId);
        try {
            SessionHistoryResponse response = interviewService.getSessionHistory(sessionId);
            return ResponseEntity.ok(ApiResponse.ok(response));
        } catch (IllegalArgumentException e) {
            log.warn("查询会话历史参数校验失败: sessionId={}, message={}", sessionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.fail("会话不存在"));
        } catch (Exception e) {
            log.error("查询会话历史时发生未预期错误: sessionId={}", sessionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("服务内部错误，请稍后重试"));
        }
    }

    /**
     * 跨会话综合总结。
     * POST /api/v1/interviews/summary/multi
     */
    @PostMapping("/api/v1/interviews/summary/multi")
    public ResponseEntity<ApiResponse<MultiSessionSummaryResponse>> generateMultiSessionSummary(
            @Valid @RequestBody MultiSessionSummaryRequest request) {
        log.info("跨会话总结: sessionIds={}", request.sessionIds());
        try {
            MultiSessionSummaryResponse response = interviewService.generateMultiSessionSummary(request);
            return ResponseEntity.ok(ApiResponse.ok(response));
        } catch (IllegalArgumentException e) {
            log.warn("跨会话总结参数校验失败: message={}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.fail("会话不存在"));
        } catch (IllegalStateException e) {
            log.warn("跨会话总结状态校验失败: message={}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.fail(e.getMessage()));
        } catch (Exception e) {
            log.error("跨会话总结时发生未预期错误", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("服务内部错误，请稍后重试"));
        }
    }
}