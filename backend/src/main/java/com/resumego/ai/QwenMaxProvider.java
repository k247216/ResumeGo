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

import java.net.SocketTimeoutException;
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
        this.aiConfig = aiConfig;
        this.objectMapper = objectMapper;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(10));
        factory.setReadTimeout(Duration.ofSeconds(60));

        this.restClient = RestClient.builder()
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
                        throw new RuntimeException("Provider error: " + res.getStatusCode());
                    })
                    .body(String.class);

            long latencyMs = System.currentTimeMillis() - start;

            String content = extractContent(responseBody);
            Integer inputTokens = extractPromptTokens(responseBody);
            Integer outputTokens = extractCompletionTokens(responseBody);

            log.info("千问 Max 调用完成: requestId={}, model={}, latencyMs={}",
                    request.requestId(), aiConfig.getModel(), latencyMs);

            return AiResult.success(request.requestId(), content, inputTokens, outputTokens, latencyMs);

        } catch (Exception e) {
            long latencyMs = System.currentTimeMillis() - start;
            AiErrorCategory category = categorizeError(e);
            log.error("千问 Max 调用失败: requestId={}, category={}, message={}",
                    request.requestId(), category, e.getMessage());
            return AiResult.failure(request.requestId(), category, e.getMessage(), latencyMs);
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
            log.warn("解析 AI 响应 content 失败，返回原始响应体", e);
        }
        return responseBody;
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

    /**
     * 将异常归类为 timeout / provider_error / unknown。
     */
    private AiErrorCategory categorizeError(Exception e) {
        if (e instanceof SocketTimeoutException
                || (e.getCause() instanceof SocketTimeoutException)) {
            return AiErrorCategory.TIMEOUT;
        }
        return AiErrorCategory.PROVIDER_ERROR;
    }
}
