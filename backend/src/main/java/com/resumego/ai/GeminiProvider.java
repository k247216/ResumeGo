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

public class GeminiProvider implements AiClient {

    private final String model;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public GeminiProvider(String baseUrl, String model, String apiKey, ObjectMapper objectMapper) {
        this(model, objectMapper, createRestClient(baseUrl, apiKey));
    }

    GeminiProvider(String model, ObjectMapper objectMapper, RestClient restClient) {
        this.model = model;
        this.objectMapper = objectMapper;
        this.restClient = restClient;
    }

    private static RestClient createRestClient(String baseUrl, String apiKey) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(60));
        return RestClient.builder().baseUrl(baseUrl)
                .defaultHeader("x-goog-api-key", apiKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(factory).build();
    }

    @Override
    public AiResult invoke(AiRequest request) {
        long start = System.currentTimeMillis();
        try {
            Map<String, Object> body = Map.of(
                    "system_instruction", Map.of("parts", List.of(Map.of("text", request.systemPrompt()))),
                    "contents", List.of(Map.of("role", "user", "parts", List.of(Map.of("text", request.userMessage()))))
            );
            String response = restClient.post().uri("/models/{model}:generateContent", model).body(body).retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new ProviderHttpException(res.getStatusCode().value());
                    }).body(String.class);
            JsonNode root = objectMapper.readTree(response);
            JsonNode text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
            if (!text.isTextual()) throw new IllegalStateException("Provider 返回格式无效");
            JsonNode usage = root.path("usageMetadata");
            return AiResult.success(request.requestId(), text.asText(),
                    integer(usage, "promptTokenCount"), integer(usage, "candidatesTokenCount"),
                    System.currentTimeMillis() - start);
        } catch (Exception exception) {
            return ProviderSupport.failure(request.requestId(), exception, start);
        }
    }

    private Integer integer(JsonNode node, String field) {
        return node.path(field).isInt() ? node.path(field).asInt() : null;
    }
}
