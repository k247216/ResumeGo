package com.resumego.ai;

/**
 * AI 调用结果。
 *
 * @param requestId     对应的请求 ID
 * @param success       调用是否成功
 * @param content       模型返回的原始文本内容
 * @param inputTokens   消耗的输入 token 数（可能为 null）
 * @param outputTokens  消耗的输出 token 数（可能为 null）
 * @param latencyMs     调用耗时（毫秒）
 * @param errorCategory 错误分类（成功时为 null）
 * @param errorMessage  错误详情（成功时为 null）
 */
public record AiResult(
        String requestId,
        boolean success,
        String content,
        Integer inputTokens,
        Integer outputTokens,
        long latencyMs,
        AiErrorCategory errorCategory,
        String errorMessage
) {
    public static AiResult success(String requestId, String content,
                                    Integer inputTokens, Integer outputTokens, long latencyMs) {
        return new AiResult(requestId, true, content, inputTokens, outputTokens,
                latencyMs, null, null);
    }

    public static AiResult failure(String requestId, AiErrorCategory errorCategory,
                                    String errorMessage, long latencyMs) {
        return new AiResult(requestId, false, null, null, null,
                latencyMs, errorCategory, errorMessage);
    }
}
