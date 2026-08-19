package com.resumego.optimization;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumego.ai.AiClient;
import com.resumego.ai.AiClientSelector;
import com.resumego.ai.AiInvocationService;
import com.resumego.ai.AiResult;
import com.resumego.ai.validate.AiOutputValidator;
import com.resumego.optimization.dto.SuggestionFollowUpRequest;
import com.resumego.optimization.dto.SuggestionFollowUpResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 建议追问服务的安全回归测试。
 *
 * <p>该服务只返回供用户人工采纳的建议，不写入简历或变更建议状态，
 * 不涉及课程规定的三个禁飞区。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SuggestionFollowUpService 单元测试")
class SuggestionFollowUpServiceTest {

    @Mock
    private OptimizationSuggestionMapper suggestionMapper;
    @Mock
    private AiClientSelector aiClientSelector;
    @Mock
    private AiClient aiClient;
    @Mock
    private AiInvocationService aiInvocationService;

    private SuggestionFollowUpService service;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        when(aiClientSelector.getClient()).thenReturn(aiClient);
        service = new SuggestionFollowUpService(
                suggestionMapper,
                aiClientSelector,
                aiInvocationService,
                new AiOutputValidator(objectMapper),
                objectMapper
        );
    }

    @Test
    @DisplayName("非法建议 ID 在调用模型前被拒绝")
    void shouldRejectInvalidSuggestionIdBeforeCallingAi() {
        assertThatIllegalArgumentException().isThrownBy(() -> service.generateFinalAdvice(
                0L, new SuggestionFollowUpRequest("补充了真实的项目实现细节")
        )).withMessageContaining("suggestionId");

        verify(aiClient, never()).invoke(any());
        verify(suggestionMapper, never()).selectById(any());
    }

    @Test
    @DisplayName("过短补充事实不查询建议也不调用模型")
    void shouldRejectShortSupplementBeforeLoadingSuggestion() {
        assertThatIllegalArgumentException().isThrownBy(() -> service.generateFinalAdvice(
                1L, new SuggestionFollowUpRequest("太短")
        )).withMessageContaining("补充事实过短");

        verify(aiClient, never()).invoke(any());
        verify(suggestionMapper, never()).selectById(any());
    }

    @Test
    @DisplayName("模型异常时返回本地建议且不修改建议记录")
    void shouldFallbackWithoutPersistingWhenAiInvocationThrows() {
        OptimizationSuggestion suggestion = suggestion();
        when(suggestionMapper.selectById(1L)).thenReturn(suggestion);
        when(aiClient.invoke(any())).thenThrow(new IllegalStateException("provider unavailable"));

        SuggestionFollowUpResponse response = service.generateFinalAdvice(
                1L, new SuggestionFollowUpRequest("我实际完成了接口异常处理和单元测试补充。")
        );

        assertThat(response.finalAdvice()).contains("手动整理");
        assertThat(response.nextSteps()).hasSize(3);
        verify(aiInvocationService).logInvocationWithSchema(any(), any(), eq(false));
        verify(suggestionMapper, never()).updateById(any(OptimizationSuggestion.class));
    }

    @Test
    @DisplayName("有效结构化输出只返回建议，不创建可直接替换的简历版本")
    void shouldReturnValidatedAdviceWithoutPersistingAnyResumeChange() {
        when(suggestionMapper.selectById(1L)).thenReturn(suggestion());
        when(aiClient.invoke(any())).thenReturn(AiResult.success(
                "request-1",
                "{\"finalAdvice\":\"请将已有项目事实整理为技术动作和可验证结果。\","
                        + "\"nextSteps\":[\"核对指标\",\"补充证据\"]}",
                1, 1, 10
        ));

        SuggestionFollowUpResponse response = service.generateFinalAdvice(
                1L, new SuggestionFollowUpRequest("我已确认项目使用 Java 完成接口开发，并保留了测试记录。")
        );

        assertThat(response.finalAdvice()).isEqualTo("请将已有项目事实整理为技术动作和可验证结果。");
        assertThat(response.nextSteps()).containsExactly("核对指标", "补充证据");
        verify(aiInvocationService).logInvocationWithSchema(any(), any(), eq(true));
        verify(suggestionMapper, never()).updateById(any(OptimizationSuggestion.class));
    }

    private OptimizationSuggestion suggestion() {
        OptimizationSuggestion suggestion = new OptimizationSuggestion();
        suggestion.setId(1L);
        suggestion.setSectionKey("projects.0.description");
        suggestion.setOriginalText("完成项目服务端开发");
        suggestion.setSuggestedText("补充异常处理和测试说明");
        suggestion.setReasonText("岗位要求体现工程质量");
        suggestion.setTargetRequirement("具备后端工程实践能力");
        suggestion.setStatus("pending");
        return suggestion;
    }
}
