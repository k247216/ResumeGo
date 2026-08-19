package com.resumego.ai;

/**
 * AI 调用客户端接口。
 * 业务模块通过此接口调用 AI，不直接依赖具体的模型供应商 SDK 或 HTTP 细节。
 */
public interface AiClient {

    /**
     * 执行一次 AI 调用。
     *
     * @param request AI 调用请求参数
     * @return AI 调用结果
     */
    AiResult invoke(AiRequest request);
}
