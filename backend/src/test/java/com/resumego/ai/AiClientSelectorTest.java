package com.resumego.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AiClientSelector 单元测试。
 */
class AiClientSelectorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("无 API Key 时返回 MockAiClient")
    void shouldReturnMockClientWhenNoApiKey() {
        AiConfig config = new AiConfig();
        config.setEndpoint("https://api.example.com");
        config.setModel("test-model");
        config.setApiKey(""); // 无 API Key

        AiClientSelector selector = new AiClientSelector(config, objectMapper);

        assertThat(selector.getClient()).isInstanceOf(MockAiClient.class);
    }

    @Test
    @DisplayName("有 API Key 时返回 QwenMaxProvider")
    void shouldReturnQwenMaxProviderWhenApiKeyPresent() {
        AiConfig config = new AiConfig();
        config.setEndpoint("https://api.example.com");
        config.setModel("test-model");
        config.setApiKey("sk-real-api-key");

        AiClientSelector selector = new AiClientSelector(config, objectMapper);

        assertThat(selector.getClient()).isInstanceOf(QwenMaxProvider.class);
    }

    @Test
    @DisplayName("空白字符串 API Key 应视为无 Key")
    void shouldTreatBlankApiKeyAsMissing() {
        AiConfig config = new AiConfig();
        config.setEndpoint("https://api.example.com");
        config.setModel("test-model");
        config.setApiKey("   ");

        AiClientSelector selector = new AiClientSelector(config, objectMapper);

        assertThat(selector.getClient()).isInstanceOf(MockAiClient.class);
    }

    @Test
    @DisplayName("getClient 多次调用返回同一实例")
    void shouldReturnSameInstanceOnMultipleCalls() {
        AiConfig config = new AiConfig();
        config.setEndpoint("https://api.example.com");
        config.setModel("test-model");
        config.setApiKey("");

        AiClientSelector selector = new AiClientSelector(config, objectMapper);

        assertThat(selector.getClient()).isSameAs(selector.getClient());
    }
}
