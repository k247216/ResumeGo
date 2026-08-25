package com.resumego.interview.controller;

import com.resumego.common.CurrentUser;
import com.resumego.interview.QuestionSourceType;
import com.resumego.interview.dto.InterviewQuestionSetRequest;
import com.resumego.interview.dto.InterviewQuestionSetResponse;
import com.resumego.interview.service.InterviewQuestionSetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 面经题集 API 边界：201/400/404/409 映射。 */
@ExtendWith(MockitoExtension.class)
@DisplayName("InterviewQuestionSetController 边界")
class InterviewQuestionSetControllerTest {

    @Mock
    private InterviewQuestionSetService questionSetService;

    private InterviewQuestionSetController controller;

    @BeforeEach
    void setUp() {
        controller = new InterviewQuestionSetController(questionSetService);
    }

    private InterviewQuestionSetRequest request() {
        return new InterviewQuestionSetRequest("题集", QuestionSourceType.USER_MANUAL, null, List.of("题目"));
    }

    private InterviewQuestionSetResponse response() {
        return new InterviewQuestionSetResponse(1L, "题集", QuestionSourceType.USER_MANUAL, null,
                false, null, LocalDateTime.now(), LocalDateTime.now(),
                List.of(new InterviewQuestionSetResponse.QuestionItem(0, "题目")));
    }

    @Test
    @DisplayName("创建成功 → 201")
    void createReturns201() {
        when(questionSetService.create(any())).thenReturn(response());
        var result = controller.create(request());
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus201());
        assertThat(result.getBody().data().id()).isEqualTo(1L);
    }

    @Test
    @DisplayName("创建校验失败 → 400")
    void createValidationFailureReturns400() {
        when(questionSetService.create(any())).thenThrow(new IllegalArgumentException("题集至少包含一道题目"));
        assertThat(controller.create(request()).getStatusCode().value()).isEqualTo(400);
    }

    @Test
    @DisplayName("详情不存在 → 404")
    void getMissingReturns404() {
        when(questionSetService.get(999L)).thenThrow(new NoSuchElementException("面经题集不存在"));
        assertThat(controller.get(999L).getStatusCode().value()).isEqualTo(404);
    }

    @Test
    @DisplayName("更新归档题集 → 409；跨用户更新 → 404")
    void updateMapping() {
        when(questionSetService.update(eq(1L), any())).thenThrow(new IllegalStateException("已归档题集不可修改"));
        assertThat(controller.update(1L, request()).getStatusCode().value()).isEqualTo(409);
        when(questionSetService.update(eq(999L), any())).thenThrow(new NoSuchElementException("面经题集不存在"));
        assertThat(controller.update(999L, request()).getStatusCode().value()).isEqualTo(404);
    }

    @Test
    @DisplayName("归档成功 → 200 并透传服务层")
    void archiveReturns200() {
        when(questionSetService.archive(1L)).thenReturn(response());
        var result = controller.archive(1L);
        assertThat(result.getStatusCode().value()).isEqualTo(200);
        verify(questionSetService).archive(1L);
    }

    @Test
    @DisplayName("列表透传")
    void listPassesThrough() {
        when(questionSetService.list()).thenReturn(List.of(response()));
        assertThat(controller.list().getBody().data()).hasSize(1);
    }

    private static org.springframework.http.HttpStatus HttpStatus201() {
        return org.springframework.http.HttpStatus.CREATED;
    }
}
