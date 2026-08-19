package com.resumego.ai.provider;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumego.ai.runtime.AiRuntimeRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AiProviderProbeServiceTest {

    private final AiProviderProbeService service = new AiProviderProbeService(
            mock(AiRuntimeRegistry.class), RestClient.builder(), new ObjectMapper());

    @Test
    void parsesAndNormalizesOpenAiModelIds() throws Exception {
        assertThat(service.parseModels("openai-compatible", """
                {"data":[{"id":"model-b"},{"id":"model-a"},{"id":"model-a"}]}
                """)).containsExactly("model-a", "model-b");
    }

    @Test
    void parsesGeminiNamesWithoutResourcePrefix() throws Exception {
        assertThat(service.parseModels("gemini", """
                {"models":[{"name":"models/gemini-2.5-pro"}]}
                """)).containsExactly("gemini-2.5-pro");
    }
}
