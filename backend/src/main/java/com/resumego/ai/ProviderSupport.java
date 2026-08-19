package com.resumego.ai;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

final class ProviderSupport {
    private ProviderSupport() {
    }

    static AiResult failure(String requestId, Exception exception, long start) {
        AiErrorCategory category = AiErrorCategory.PROVIDER_ERROR;
        if (exception instanceof ProviderHttpException http) {
            if (http.status() == 401) category = AiErrorCategory.AUTHENTICATION;
            else if (http.status() == 403) category = AiErrorCategory.PERMISSION;
            else if (http.status() == 429) category = AiErrorCategory.RATE_LIMIT;
        } else if (hasCause(exception, SocketTimeoutException.class)) {
            category = AiErrorCategory.TIMEOUT;
        } else if (hasCause(exception, ConnectException.class)) {
            category = AiErrorCategory.NETWORK;
        } else if (exception instanceof IllegalStateException) {
            category = AiErrorCategory.INVALID_RESPONSE;
        }
        return AiResult.failure(requestId, category, safeMessage(category), System.currentTimeMillis() - start);
    }

    private static boolean hasCause(Throwable throwable, Class<? extends Throwable> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) return true;
            current = current.getCause();
        }
        return false;
    }

    private static String safeMessage(AiErrorCategory category) {
        return switch (category) {
            case AUTHENTICATION -> "API Key 无效或无权访问该模型";
            case PERMISSION -> "API Key 无权访问该模型";
            case RATE_LIMIT -> "模型服务请求受限，请稍后重试";
            case TIMEOUT -> "模型服务响应超时";
            case NETWORK -> "无法连接模型服务";
            case INVALID_RESPONSE -> "模型服务返回格式无效";
            default -> "模型服务调用失败";
        };
    }
}
