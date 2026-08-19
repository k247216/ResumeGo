package com.resumego.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * AI 调用审计日志服务。
 * <p>
 * 每次 AI 调用（成功或失败）均写入 ai_invocations 表。
 * 日志中不包含完整简历原文和 API Key。
 */
@Service
public class AiInvocationService {

    private static final Logger log = LoggerFactory.getLogger(AiInvocationService.class);

    private final AiInvocationMapper aiInvocationMapper;
    private final AiConfig aiConfig;

    public AiInvocationService(AiInvocationMapper aiInvocationMapper, AiConfig aiConfig) {
        this.aiInvocationMapper = aiInvocationMapper;
        this.aiConfig = aiConfig;
    }

    /**
     * 记录一次 AI 调用。
     *
     * @param request AI 请求
     * @param result  AI 结果
     */
    @Transactional
    public void logInvocation(AiRequest request, AiResult result) {
        AiInvocation entity = new AiInvocation();
        entity.setRequestId(request.requestId());
        entity.setUserId(request.userId());
        entity.setFeatureType(request.featureType());
        entity.setProvider(aiConfig.getModel() != null && aiConfig.isApiKeyConfigured()
                ? "qwen" : "mock");
        entity.setModelName(aiConfig.isApiKeyConfigured() ? aiConfig.getModel() : "mock");
        entity.setPromptVersion(request.promptVersion());
        entity.setStatus(result.success() ? "success" : "failed");
        entity.setLatencyMs(result.latencyMs() > Integer.MAX_VALUE
                ? Integer.MAX_VALUE : (int) result.latencyMs());
        entity.setInputTokens(result.inputTokens());
        entity.setOutputTokens(result.outputTokens());
        entity.setErrorCategory(result.errorCategory() != null
                ? result.errorCategory().name() : null);
        entity.setPromptSummary(truncate(request.systemPrompt(), 200));
        entity.setResponseSummary(truncate(result.content(), 200));

        aiInvocationMapper.insert(entity);

        // 审计日志：仅记录元数据，不记录 userMessage（业务数据）
        log.info("AI 审计: requestId={}, featureType={}, provider={}, model={}, "
                        + "status={}, latencyMs={}, errorCategory={}",
                request.requestId(), request.featureType(),
                entity.getProvider(), entity.getModelName(),
                entity.getStatus(), entity.getLatencyMs(), entity.getErrorCategory());
    }

    /**
     * 记录一次 AI 调用，同时写入 Schema 校验结果。
     */
    @Transactional
    public void logInvocationWithSchema(AiRequest request, AiResult result, boolean schemaValid) {
        AiInvocation entity = new AiInvocation();
        entity.setRequestId(request.requestId());
        entity.setUserId(request.userId());
        entity.setFeatureType(request.featureType());
        entity.setProvider(aiConfig.isApiKeyConfigured() ? "qwen" : "mock");
        entity.setModelName(aiConfig.isApiKeyConfigured() ? aiConfig.getModel() : "mock");
        entity.setPromptVersion(request.promptVersion());
        entity.setStatus(result.success() ? "success" : "failed");
        entity.setLatencyMs(result.latencyMs() > Integer.MAX_VALUE
                ? Integer.MAX_VALUE : (int) result.latencyMs());
        entity.setSchemaValid(schemaValid);
        entity.setInputTokens(result.inputTokens());
        entity.setOutputTokens(result.outputTokens());
        entity.setErrorCategory(result.errorCategory() != null
                ? result.errorCategory().name() : null);
        entity.setPromptSummary(truncate(request.systemPrompt(), 200));
        entity.setResponseSummary(truncate(result.content(), 200));

        aiInvocationMapper.insert(entity);

        log.info("AI 审计: requestId={}, featureType={}, provider={}, model={}, "
                        + "status={}, schemaValid={}, latencyMs={}, errorCategory={}",
                request.requestId(), request.featureType(),
                entity.getProvider(), entity.getModelName(),
                entity.getStatus(), schemaValid, entity.getLatencyMs(),
                entity.getErrorCategory());
    }

    /**
     * 截断文本到指定长度，null 安全。
     */
    private static String truncate(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }
}
