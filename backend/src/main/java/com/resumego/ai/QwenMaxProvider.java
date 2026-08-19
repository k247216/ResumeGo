package com.resumego.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.Map;

/**
 * 千问 Max Provider，通过阿里云百炼兼容 OpenAI 接口调用千问 Max 模型。
 */
public class QwenMaxProvider implements AiClient {

    private static final Logger log = LoggerFactory.getLogger(QwenMaxProvider.class);

    private final AiConfig aiConfig;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public QwenMaxProvider(AiConfig aiConfig, ObjectMapper objectMapper) {
        this(aiConfig, objectMapper, createRestClient(aiConfig));
    }

    QwenMaxProvider(AiConfig aiConfig, ObjectMapper objectMapper, RestClient restClient) {
        this.aiConfig = aiConfig;
        this.objectMapper = objectMapper;
        this.restClient = restClient;
    }

    private static RestClient createRestClient(AiConfig aiConfig) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(60));

        return RestClient.builder()
                .baseUrl(aiConfig.getEndpoint())
                .defaultHeader("Authorization", "Bearer " + aiConfig.getApiKey())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(factory)
                .build();
    }

    @Override
    public AiResult invoke(AiRequest request) {
        long start = System.currentTimeMillis();

        try {
            Map<String, Object> body = Map.of(
                    "model", aiConfig.getModel(),
                    "messages", new Object[]{
                            Map.of("role", "system", "content", request.systemPrompt()),
                            Map.of("role", "user", "content", request.userMessage())
                    }
            );

            String responseBody = restClient.post()
                    .uri("/chat/completions")
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, res) -> {
                        throw new ProviderHttpException(res.getStatusCode().value());
                    })
                    .body(String.class);

            long latencyMs = System.currentTimeMillis() - start;

            String content = extractContent(responseBody);
            Integer inputTokens = extractPromptTokens(responseBody);
            Integer outputTokens = extractCompletionTokens(responseBody);

            log.info("OpenAI 兼容模型调用完成: requestId={}, model={}, latencyMs={}",
                    request.requestId(), aiConfig.getModel(), latencyMs);

            return AiResult.success(request.requestId(), content, inputTokens, outputTokens, latencyMs);

        } catch (Exception e) {
            AiResult failure = ProviderSupport.failure(request.requestId(), e, start);
            log.warn("OpenAI 兼容模型调用失败: requestId={}, category={}",
                    request.requestId(), failure.errorCategory());
            return failure;
        }
    }

    /**
     * 从 OpenAI 兼容响应中提取 content 文本。
     */
    private String extractContent(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).get("message");
                if (message != null) {
                    JsonNode content = message.get("content");
                    if (content != null) {
                        return content.asText();
                    }
                }
            }
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Provider 返回格式无效", e);
        }
        throw new IllegalStateException("Provider 返回格式无效");
    }

    /**
     * 从响应中提取 prompt_tokens。
     */
    private Integer extractPromptTokens(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode usage = root.get("usage");
            if (usage != null) {
                JsonNode promptTokens = usage.get("prompt_tokens");
                if (promptTokens != null && promptTokens.isInt()) {
                    return promptTokens.asInt();
                }
            }
        } catch (JsonProcessingException e) {
            log.warn("解析 token 用量失败", e);
        }
        return null;
    }

    /**
     * 从响应中提取 completion_tokens。
     */
    private Integer extractCompletionTokens(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode usage = root.get("usage");
            if (usage != null) {
                JsonNode completionTokens = usage.get("completion_tokens");
                if (completionTokens != null && completionTokens.isInt()) {
                    return completionTokens.asInt();
                }
            }
        } catch (JsonProcessingException e) {
            log.warn("解析 token 用量失败", e);
        }
        return null;
    }

}
