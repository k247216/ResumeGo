package com.resumego.interview.controller;

import com.resumego.common.ApiResponse;
import com.resumego.interview.dto.InterviewStatusResponse;
import com.resumego.interview.dto.MultiSessionSummaryRequest;
import com.resumego.interview.dto.MultiSessionSummaryResponse;
import com.resumego.interview.dto.SessionHistoryResponse;
import com.resumego.interview.dto.StartInterviewRequest;
import com.resumego.interview.dto.SubmitAnswerRequest;
import com.resumego.interview.dto.SubmitAnswerResponse;
import com.resumego.interview.service.InterviewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/** Controller 协议映射测试；不覆盖面试状态机规则。 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InterviewController 协议映射测试")
class InterviewControllerTest {

    @Mock
    private InterviewService interviewService;

    @Test
    @DisplayName("创建会话成功时返回 201")
    void shouldCreateInterview() {
        when(interviewService.createInterview(any())).thenReturn(status(1L));

        ResponseEntity<ApiResponse<InterviewStatusResponse>> response = controller().createInterview(request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).extracting(ApiResponse::success).isEqualTo(true);
    }

    @Test
    @DisplayName("创建参数无效和服务异常应返回对应协议错误")
    void shouldMapCreateFailures() {
        when(interviewService.createInterview(any())).thenThrow(new IllegalArgumentException("不存在"));
        assertThat(controller().createInterview(request()).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        reset(interviewService);
        when(interviewService.createInterview(any())).thenThrow(new RuntimeException("down"));
        ResponseEntity<ApiResponse<InterviewStatusResponse>> response = controller().createInterview(request());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().message()).contains("稍后重试");
    }

    @Test
    @DisplayName("开始与查询状态应透传成功响应")
    void shouldStartAndGetStatus() {
        when(interviewService.startInterview(1L)).thenReturn(status(1L));
        when(interviewService.getInterviewStatus(1L)).thenReturn(status(1L));

        assertThat(controller().startInterview(1L).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller().getInterviewStatus(1L).getBody().data().sessionId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("开始与查询的服务异常应映射为非状态机的 HTTP 错误")
    void shouldMapStartAndStatusFailures() {
        when(interviewService.startInterview(1L)).thenThrow(new IllegalArgumentException("不存在"));
        assertThat(controller().startInterview(1L).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        reset(interviewService);
        when(interviewService.startInterview(1L)).thenThrow(new IllegalStateException("业务拒绝"));
        assertThat(controller().startInterview(1L).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        reset(interviewService);
        when(interviewService.getInterviewStatus(1L)).thenThrow(new RuntimeException("down"));
        assertThat(controller().getInterviewStatus(1L).getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("提交回答成功与失败时应返回统一协议结构")
    void shouldMapSubmitAnswerResponses() {
        SubmitAnswerResponse answer = new SubmitAnswerResponse(1L, "WAITING_ANSWER", 1, 3,
                null, null, false, false);
        when(interviewService.submitAnswer(any(), any())).thenReturn(answer);
        assertThat(controller().submitAnswer(1L, new SubmitAnswerRequest("回答")).getStatusCode())
                .isEqualTo(HttpStatus.OK);

        when(interviewService.submitAnswer(any(), any())).thenThrow(new IllegalArgumentException("参数"));
        assertThat(controller().submitAnswer(1L, new SubmitAnswerRequest("回答")).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
        reset(interviewService);
        when(interviewService.submitAnswer(any(), any())).thenThrow(new RuntimeException("down"));
        assertThat(controller().submitAnswer(1L, new SubmitAnswerRequest("回答")).getStatusCode())
                .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @DisplayName("列表和历史接口应映射成功与服务异常")
    void shouldMapListAndHistoryResponses() {
        when(interviewService.listMyInterviews()).thenReturn(List.of(status(1L)));
        when(interviewService.getSessionHistory(1L))
                .thenReturn(new SessionHistoryResponse(1L, List.of()));
        assertThat(controller().listMyInterviews().getBody().data()).hasSize(1);
        assertThat(controller().getSessionHistory(1L).getStatusCode()).isEqualTo(HttpStatus.OK);

        reset(interviewService);
        when(interviewService.listMyInterviews()).thenThrow(new RuntimeException("down"));
        assertThat(controller().listMyInterviews().getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        reset(interviewService);
        when(interviewService.getSessionHistory(1L)).thenThrow(new IllegalArgumentException("不存在"));
        assertThat(controller().getSessionHistory(1L).getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("跨会话总结接口应映射成功、校验失败和未预期错误")
    void shouldMapMultiSessionSummaryResponses() {
        MultiSessionSummaryRequest request = new MultiSessionSummaryRequest(List.of(1L));
        MultiSessionSummaryResponse summary = new MultiSessionSummaryResponse(
                "总结", 80, List.of(), List.of(), List.of(), List.of());
        when(interviewService.generateMultiSessionSummary(request)).thenReturn(summary);
        assertThat(controller().generateMultiSessionSummary(request).getStatusCode()).isEqualTo(HttpStatus.OK);

        when(interviewService.generateMultiSessionSummary(request)).thenThrow(new IllegalStateException("无法总结"));
        assertThat(controller().generateMultiSessionSummary(request).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        reset(interviewService);
        when(interviewService.generateMultiSessionSummary(request)).thenThrow(new RuntimeException("down"));
        assertThat(controller().generateMultiSessionSummary(request).getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private InterviewController controller() {
        return new InterviewController(interviewService);
    }

    private StartInterviewRequest request() {
        return new StartInterviewRequest(10L, 20L, 5, 1L);
    }

    private InterviewStatusResponse status(Long id) {
        return new InterviewStatusResponse(id, "READY", 0, 5, null, null, false, List.of(), "面试官", "岗位");
    }
}
