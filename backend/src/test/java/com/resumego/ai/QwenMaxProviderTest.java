package com.resumego.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * QwenMaxProvider 单元测试。
 * 使用不可达的测试端点验证错误处理逻辑。
 */
class QwenMaxProviderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("真实调用失败时应返回失败结果并保留 requestId")
    void shouldReturnFailureAndPreserveRequestId() {
        AiConfig config = new AiConfig();
        config.setEndpoint("https://api.example.com/v1");
        config.setModel("qwen-max");
        config.setApiKey("sk-test");

        QwenMaxProvider provider = new QwenMaxProvider(config, objectMapper);

        AiRequest request = AiRequest.builder()
                .featureType("jd_parse")
                .userId(1L)
                .promptVersion("v1")
                .systemPrompt("解析JD")
                .userMessage("岗位要求 Java")
                .build();

        AiResult result = provider.invoke(request);

        assertThat(result.success()).isFalse();
        assertThat(result.requestId()).isEqualTo(request.requestId());
        assertThat(result.errorCategory()).isEqualTo(AiErrorCategory.PROVIDER_ERROR);
    }

    @Test
    @DisplayName("调用失败时 requestId 应与请求一致")
    void shouldPreserveRequestIdOnFailure() {
        AiConfig config = new AiConfig();
        config.setEndpoint("https://api.example.com/v1");
        config.setModel("qwen-max");
        config.setApiKey("sk-test");

        QwenMaxProvider provider = new QwenMaxProvider(config, objectMapper);

        AiRequest request = AiRequest.builder()
                .requestId("custom-req-123")
                .featureType("jd_parse")
                .userId(1L)
                .promptVersion("v1")
                .systemPrompt("")
                .userMessage("")
                .build();

        AiResult result = provider.invoke(request);

        assertThat(result.requestId()).isEqualTo("custom-req-123");
    }
}
