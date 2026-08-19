package com.resumego.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumego.ai.runtime.AiRuntimeRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiClientSelectorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void returnsNotConfiguredInsteadOfDisguisedMockOutput() {
        AiConfig config = config("");
        AiRuntimeRegistry registry = new AiRuntimeRegistry(objectMapper);
        AiClientSelector selector = new AiClientSelector(config, objectMapper, registry);

        AiResult result = selector.invoke(request());

        assertThat(result.success()).isFalse();
        assertThat(result.errorCategory()).isEqualTo(AiErrorCategory.NOT_CONFIGURED);
        assertThat(selector.getClient()).isSameAs(selector);
    }

    @Test
    void loadsLegacyEnvironmentConfigurationIntoRuntimeRegistry() {
        AiConfig config = config("sk-legacy");
        AiRuntimeRegistry registry = new AiRuntimeRegistry(objectMapper);

        new AiClientSelector(config, objectMapper, registry);

        assertThat(registry.activeRuntime()).isNotNull();
        assertThat(registry.activeRuntime().protocolType()).isEqualTo("openai-compatible");
        assertThat(registry.activeRuntime().model()).isEqualTo("test-model");
    }

    private AiConfig config(String apiKey) {
        AiConfig config = new AiConfig();
        config.setEndpoint("https://api.example.com/v1");
        config.setModel("test-model");
        config.setApiKey(apiKey);
        return config;
    }

    private AiRequest request() {
        return AiRequest.builder().requestId("request-1").featureType("test")
                .systemPrompt("system").userMessage("user").build();
    }
}
