package com.resumego.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;

public class AnthropicProvider implements AiClient {

    private final String model;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public AnthropicProvider(String baseUrl, String model, String apiKey, ObjectMapper objectMapper) {
        this(model, objectMapper, createRestClient(baseUrl, apiKey));
    }

    AnthropicProvider(String model, ObjectMapper objectMapper, RestClient restClient) {
        this.model = model;
        this.objectMapper = objectMapper;
        this.restClient = restClient;
    }

    private static RestClient createRestClient(String baseUrl, String apiKey) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(60));
        return RestClient.builder().baseUrl(baseUrl)
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", "2023-06-01")
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(factory).build();
    }

    @Override
    public AiResult invoke(AiRequest request) {
        long start = System.currentTimeMillis();
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "max_tokens", request.parameters().getOrDefault("max_tokens", 4096),
                    "system", request.systemPrompt(),
                    "messages", List.of(Map.of("role", "user", "content", request.userMessage()))
            );
            String response = restClient.post().uri("/messages").body(body).retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new ProviderHttpException(res.getStatusCode().value());
                    }).body(String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode content = root.path("content");
            if (!content.isArray() || content.isEmpty() || !content.get(0).hasNonNull("text")) {
                throw new IllegalStateException("Provider 返回格式无效");
            }
            return AiResult.success(request.requestId(), content.get(0).path("text").asText(),
                    integer(root.path("usage"), "input_tokens"), integer(root.path("usage"), "output_tokens"),
                    System.currentTimeMillis() - start);
        } catch (Exception exception) {
            return ProviderSupport.failure(request.requestId(), exception, start);
        }
    }

    private Integer integer(JsonNode node, String field) {
        return node.path(field).isInt() ? node.path(field).asInt() : null;
    }
}
