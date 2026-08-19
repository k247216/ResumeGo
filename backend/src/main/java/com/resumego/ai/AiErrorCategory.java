package com.resumego.ai;

/**
 * AI 调用错误分类。
 */
public enum AiErrorCategory {

    /** 请求超时 */
    TIMEOUT,

    /** 模型返回的 JSON 无法解析或不符合 Schema */
    INVALID_JSON,

    /** AI 服务商返回错误（认证失败、限流、服务不可用等） */
    PROVIDER_ERROR,

    /** 未知错误 */
    UNKNOWN
}
