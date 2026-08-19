package com.resumego.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumego.ai.provider.AiProviderProfile;
import com.resumego.ai.runtime.AiRuntimeRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Stable business-facing client that delegates every invocation to the current
 * in-memory provider. This keeps provider changes effective without rebuilding services.
 */
@Component
public class AiClientSelector implements AiClient {

    private static final Logger log = LoggerFactory.getLogger(AiClientSelector.class);
    private final AiRuntimeRegistry registry;

    public AiClientSelector(AiConfig legacyConfig, ObjectMapper objectMapper, AiRuntimeRegistry registry) {
        this.registry = registry;
        if (legacyConfig.isApiKeyConfigured() && registry.activeRuntime() == null) {
            LocalDateTime now = LocalDateTime.now();
            registry.apply(new AiProviderProfile(0L, 1L, "环境变量配置", "openai-compatible",
                    legacyConfig.getEndpoint(), legacyConfig.getModel(), true,
                    null, null, null, now, now), legacyConfig.getApiKey());
            log.info("已装载环境变量中的兼容模型配置 (model={})", legacyConfig.getModel());
        } else if (registry.activeRuntime() == null) {
            log.info("AI 模式: 未配置；本地编辑功能保持可用");
        }
    }

    public AiClient getClient() {
        return this;
    }

    @Override
    public AiResult invoke(AiRequest request) {
        return registry.client().invoke(request);
    }
}
