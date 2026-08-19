package com.resumego.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class ProviderProtocolTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void sendsOpenAiCompatibleChatCompletion() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://provider.test/v1")
                .defaultHeader("Authorization", "Bearer secret-key");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://provider.test/v1/chat/completions"))
                .andExpect(header("Authorization", "Bearer secret-key"))
                .andExpect(content().string(allOf(containsString("compatible-model"),
                        containsString("system prompt"), containsString("user message"))))
                .andRespond(withSuccess("""
                        {"choices":[{"message":{"content":"OK"}}],"usage":{"prompt_tokens":2,"completion_tokens":1}}
                        """, MediaType.APPLICATION_JSON));
        AiConfig config = new AiConfig();
        config.setEndpoint("https://provider.test");
        config.setModel("compatible-model");
        config.setApiKey("secret-key");

        AiResult result = new QwenMaxProvider(config, objectMapper, builder.build()).invoke(request());

        assertThat(result.success()).isTrue();
        assertThat(result.content()).isEqualTo("OK");
        server.verify();
    }

    @Test
    void sendsAnthropicMessagesRequest() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://provider.test/v1")
                .defaultHeader("x-api-key", "anthropic-key");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://provider.test/v1/messages"))
                .andExpect(header("x-api-key", "anthropic-key"))
                .andExpect(content().string(allOf(containsString("claude-test"),
                        containsString("system prompt"), containsString("user message"))))
                .andRespond(withSuccess("""
                        {"content":[{"type":"text","text":"OK"}],"usage":{"input_tokens":2,"output_tokens":1}}
                        """, MediaType.APPLICATION_JSON));

        AiResult result = new AnthropicProvider("claude-test", objectMapper, builder.build()).invoke(request());

        assertThat(result.success()).isTrue();
        assertThat(result.content()).isEqualTo("OK");
        server.verify();
    }

    @Test
    void sendsGeminiGenerateContentRequestWithoutPuttingKeyInUrl() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://provider.test/v1beta")
                .defaultHeader("x-goog-api-key", "gemini-key");
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://provider.test/v1beta/models/gemini-test:generateContent"))
                .andExpect(header("x-goog-api-key", "gemini-key"))
                .andExpect(content().string(allOf(containsString("system_instruction"),
                        containsString("system prompt"), containsString("user message"))))
                .andRespond(withSuccess("""
                        {"candidates":[{"content":{"parts":[{"text":"OK"}]}}],"usageMetadata":{"promptTokenCount":2,"candidatesTokenCount":1}}
                        """, MediaType.APPLICATION_JSON));

        AiResult result = new GeminiProvider("gemini-test", objectMapper, builder.build()).invoke(request());

        assertThat(result.success()).isTrue();
        assertThat(result.content()).isEqualTo("OK");
        server.verify();
    }

    private AiRequest request() {
        return AiRequest.builder().requestId("protocol-test").featureType("test")
                .systemPrompt("system prompt").userMessage("user message").build();
    }
}
