package com.resumego.ai.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumego.ai.*;
import com.resumego.ai.provider.AiProviderProfile;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
public class AiRuntimeRegistry {

    public record ActiveRuntime(long profileId, String protocolType, String baseUrl, String model, AiClient client) {
    }

    private final ObjectMapper objectMapper;
    private final AtomicReference<ActiveRuntime> active = new AtomicReference<>();

    public AiRuntimeRegistry(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void apply(AiProviderProfile profile, String apiKey) {
        active.set(new ActiveRuntime(profile.id(), profile.protocolType(), profile.baseUrl(), profile.defaultModel(),
                clientFor(profile, apiKey)));
    }

    public AiClient clientFor(AiProviderProfile profile, String apiKey) {
        if (apiKey == null || apiKey.isBlank()) throw new IllegalArgumentException("API Key 不能为空");
        return switch (profile.protocolType()) {
            case "openai-compatible" -> openAiCompatible(profile, apiKey.trim());
            case "anthropic" -> new AnthropicProvider(profile.baseUrl(), profile.defaultModel(), apiKey.trim(), objectMapper);
            case "gemini" -> new GeminiProvider(profile.baseUrl(), profile.defaultModel(), apiKey.trim(), objectMapper);
            default -> throw new IllegalArgumentException("不支持的模型协议: " + profile.protocolType());
        };
    }

    public void clear(long profileId) {
        active.updateAndGet(current -> current != null && current.profileId() == profileId ? null : current);
    }

    public void clearActive() {
        active.set(null);
    }

    public boolean hasKey(long profileId) {
        ActiveRuntime current = active.get();
        return current != null && current.profileId() == profileId;
    }

    public ActiveRuntime activeRuntime() {
        return active.get();
    }

    public AiClient client() {
        ActiveRuntime current = active.get();
        if (current != null) return current.client();
        return request -> AiResult.failure(request.requestId(), AiErrorCategory.NOT_CONFIGURED,
                "尚未配置 AI 模型服务", 0L);
    }

    private AiClient openAiCompatible(AiProviderProfile profile, String apiKey) {
        AiConfig config = new AiConfig();
        config.setEndpoint(profile.baseUrl());
        config.setModel(profile.defaultModel());
        config.setApiKey(apiKey);
        return new QwenMaxProvider(config, objectMapper);
    }
}
