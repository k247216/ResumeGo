package com.resumego.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumego.ai.AiRequest;
import com.resumego.ai.AiResult;
import com.resumego.ai.runtime.AiRuntimeRegistry;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.time.Duration;

@Service
public class AiProviderProbeService {

    private final AiRuntimeRegistry registry;
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    public AiProviderProbeService(AiRuntimeRegistry registry, RestClient.Builder restClientBuilder,
                                  ObjectMapper objectMapper) {
        this.registry = registry;
        this.restClientBuilder = restClientBuilder;
        this.objectMapper = objectMapper;
    }

    public AiProviderProbeResponse test(AiProviderProfile profile, String apiKey) {
        AiResult result = registry.clientFor(profile, apiKey).invoke(AiRequest.builder()
                .featureType("provider_connection_test")
                .userId(profile.userId())
                .promptVersion("provider-test-v1")
                .systemPrompt("Return exactly OK. Do not include any other text.")
                .userMessage("Connection test")
                .build());
        return new AiProviderProbeResponse(result.success(),
                result.success() ? "连接成功" : safeMessage(result), List.of());
    }

    public AiProviderProbeResponse models(AiProviderProfile profile, String apiKey) {
        requireKey(apiKey);
        try {
            SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
            requestFactory.setConnectTimeout(Duration.ofSeconds(10));
            requestFactory.setReadTimeout(Duration.ofSeconds(30));
            RestClient.Builder builder = restClientBuilder.clone().baseUrl(profile.baseUrl())
                    .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE)
                    .requestFactory(requestFactory);
            switch (profile.protocolType()) {
                case "openai-compatible" -> builder.defaultHeader("Authorization", "Bearer " + apiKey.trim());
                case "anthropic" -> builder.defaultHeader("x-api-key", apiKey.trim())
                        .defaultHeader("anthropic-version", "2023-06-01");
                case "gemini" -> builder.defaultHeader("x-goog-api-key", apiKey.trim());
                default -> throw new IllegalArgumentException("不支持的模型协议");
            }
            String body = builder.build().get().uri("/models").retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new IllegalStateException(response.getStatusCode().value() == 401
                                ? "API Key 无效" : "获取模型失败");
                    }).body(String.class);
            List<String> models = parseModels(profile.protocolType(), body);
            return new AiProviderProbeResponse(true, "已获取 " + models.size() + " 个模型", models);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("无法获取模型列表；你仍可手工填写模型名称");
        }
    }

    List<String> parseModels(String protocol, String body) throws Exception {
        JsonNode root = objectMapper.readTree(body);
        JsonNode values = "gemini".equals(protocol) ? root.path("models") : root.path("data");
        if (!values.isArray()) throw new IllegalStateException("模型列表返回格式无效");
        List<String> models = new ArrayList<>();
        for (JsonNode value : values) {
            String id = value.path("id").asText(value.path("name").asText(""));
            if (id.startsWith("models/")) id = id.substring("models/".length());
            if (!id.isBlank() && id.length() <= 200) models.add(id);
            if (models.size() >= 200) break;
        }
        return models.stream().distinct().sorted().toList();
    }

    private String safeMessage(AiResult result) {
        return result.errorMessage() == null ? "连接失败" : result.errorMessage();
    }

    private void requireKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) throw new IllegalArgumentException("API Key 不能为空");
    }
}
