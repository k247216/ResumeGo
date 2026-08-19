package com.resumego.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * AI 客户端选择器。
 * <p>
 * 根据配置决定使用真实 AI Provider 还是 Mock：
 * <ul>
 *     <li>有 API Key → QwenMaxProvider（千问 Max 真实调用）</li>
 *     <li>无 API Key → MockAiClient（本地模拟，不发起网络请求）</li>
 * </ul>
 */
@Component
public class AiClientSelector {

    private static final Logger log = LoggerFactory.getLogger(AiClientSelector.class);

    private final AiClient aiClient;

    public AiClientSelector(AiConfig aiConfig, ObjectMapper objectMapper) {
        if (aiConfig.isApiKeyConfigured()) {
            log.info("AI 模式: 千问 Max 真实调用 (model={})", aiConfig.getModel());
            this.aiClient = new QwenMaxProvider(aiConfig, objectMapper);
        } else {
            log.info("AI 模式: Mock 本地模拟 (未配置 API Key)");
            this.aiClient = new MockAiClient(objectMapper);
        }
    }

    /**
     * 获取当前生效的 AI 客户端。
     *
     * @return AiClient 实例（QwenMaxProvider 或 MockAiClient）
     */
    public AiClient getClient() {
        return aiClient;
    }
}
