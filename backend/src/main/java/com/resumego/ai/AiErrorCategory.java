package com.resumego.ai;

/**
 * AI 调用错误分类。
 */
public enum AiErrorCategory {

    /** 用户尚未配置可用模型 */
    NOT_CONFIGURED,

    /** API Key 无效 */
    AUTHENTICATION,

    /** API Key 有效但无权访问当前资源 */
    PERMISSION,

    /** 请求频率或额度受限 */
    RATE_LIMIT,

    /** 网络连接失败 */
    NETWORK,

    /** Provider 响应缺少预期结构 */
    INVALID_RESPONSE,

    /** 请求超时 */
    TIMEOUT,

    /** 模型返回的 JSON 无法解析或不符合 Schema */
    INVALID_JSON,

    /** AI 服务商返回错误（认证失败、限流、服务不可用等） */
    PROVIDER_ERROR,

    /** 未知错误 */
    UNKNOWN
}
