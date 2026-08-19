package com.resumego.optimization;

import com.resumego.common.ApiResponse;
import com.resumego.optimization.dto.GenerateSuggestionsResponse;
import com.resumego.optimization.dto.SuggestionFollowUpRequest;
import com.resumego.optimization.dto.SuggestionFollowUpResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * 优化建议接口的异常映射回归测试。
 *
 * <p>仅验证 Controller 的 HTTP 响应契约，不涉及评分、岗位匹配排序或面试状态机。</p>
 */
@ExtendWith(MockitoExtension.class)
class OptimizationSuggestionControllerTest {

    @Mock
    private OptimizationSuggestionService suggestionService;

    @Mock
    private SuggestionFollowUpService followUpService;

    @InjectMocks
    private OptimizationSuggestionController controller;

    @Test
    void generateSuggestionsShouldReturnNotFoundForMissingMatch() {
        when(suggestionService.generateSuggestions(404L))
                .thenThrow(new IllegalArgumentException("match not found"));

        ResponseEntity<ApiResponse<GenerateSuggestionsResponse>> response = controller.generateSuggestions(404L);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertFalse(response.getBody().success());
        assertEquals("请求参数无效，请检查输入", response.getBody().message());
    }

    @Test
    void generateSuggestionsShouldReturnBadRequestForStateConflict() {
        when(suggestionService.generateSuggestions(1L))
                .thenThrow(new IllegalStateException("already processed"));

        ResponseEntity<ApiResponse<GenerateSuggestionsResponse>> response = controller.generateSuggestions(1L);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().success());
        assertEquals("当前状态不允许此操作", response.getBody().message());
    }

    @Test
    void generateSuggestionsShouldNotExposeExceptionDetail() {
        when(suggestionService.generateSuggestions(1L))
                .thenThrow(new RuntimeException("internal database detail"));

        ResponseEntity<ApiResponse<GenerateSuggestionsResponse>> response = controller.generateSuggestions(1L);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().success());
        assertEquals("服务内部错误，请稍后重试", response.getBody().message());
    }

    @Test
    void getSuggestionsShouldReturnInternalErrorWhenServiceFails() {
        when(suggestionService.getSuggestions(1L))
                .thenThrow(new RuntimeException("internal database detail"));

        ResponseEntity<ApiResponse<GenerateSuggestionsResponse>> response = controller.getSuggestions(1L);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertFalse(response.getBody().success());
        assertEquals("服务内部错误，请稍后重试", response.getBody().message());
    }

    @Test
    void followUpShouldReturnBadRequestForInvalidSupplement() {
        SuggestionFollowUpRequest request = new SuggestionFollowUpRequest("已有事实补充说明");
        when(followUpService.generateFinalAdvice(1L, request))
                .thenThrow(new IllegalArgumentException("补充事实不符合要求"));

        ResponseEntity<ApiResponse<SuggestionFollowUpResponse>> response = controller.generateFollowUpAdvice(1L, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().success());
        assertEquals("补充事实不符合要求", response.getBody().message());
    }

    @Test
    void acceptSuggestionShouldReturnBadRequestForStateConflict() {
        doThrow(new IllegalStateException("already decided"))
                .when(suggestionService).acceptSuggestion(1L);

        ResponseEntity<ApiResponse<Void>> response = controller.acceptSuggestion(1L);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertFalse(response.getBody().success());
        assertEquals("当前状态不允许此操作", response.getBody().message());
    }

    @Test
    void generateSuggestionsShouldReturnServiceDataOnSuccess() {
        GenerateSuggestionsResponse result = new GenerateSuggestionsResponse(List.of());
        when(suggestionService.generateSuggestions(1L)).thenReturn(result);

        ResponseEntity<ApiResponse<GenerateSuggestionsResponse>> response = controller.generateSuggestions(1L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertSame(result, response.getBody().data());
    }
}
