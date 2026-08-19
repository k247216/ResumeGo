package com.resumego.ai;

import java.util.Map;
import java.util.UUID;

/**
 * AI 调用请求，封装一次 AI 调用的全部参数。
 *
 * @param requestId     请求唯一标识（用于审计链路追踪）
 * @param featureType   功能类型（如 "jd_parse", "resume_optimization"）
 * @param userId        发起调用的用户 ID
 * @param promptVersion Prompt 版本号
 * @param systemPrompt  系统提示词
 * @param userMessage   用户消息（业务数据，不得包含完整简历原文）
 * @param parameters    模型参数（temperature, max_tokens 等，可选）
 */
public record AiRequest(
        String requestId,
        String featureType,
        Long userId,
        String promptVersion,
        String systemPrompt,
        String userMessage,
        Map<String, Object> parameters
) {
    public AiRequest {
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String requestId = UUID.randomUUID().toString();
        private String featureType;
        private Long userId;
        private String promptVersion;
        private String systemPrompt;
        private String userMessage;
        private Map<String, Object> parameters = Map.of();

        public Builder requestId(String requestId) {
            this.requestId = requestId;
            return this;
        }

        public Builder featureType(String featureType) {
            this.featureType = featureType;
            return this;
        }

        public Builder userId(Long userId) {
            this.userId = userId;
            return this;
        }

        public Builder promptVersion(String promptVersion) {
            this.promptVersion = promptVersion;
            return this;
        }

        public Builder systemPrompt(String systemPrompt) {
            this.systemPrompt = systemPrompt;
            return this;
        }

        public Builder userMessage(String userMessage) {
            this.userMessage = userMessage;
            return this;
        }

        public Builder parameters(Map<String, Object> parameters) {
            this.parameters = parameters != null ? Map.copyOf(parameters) : Map.of();
            return this;
        }

        public AiRequest build() {
            return new AiRequest(requestId, featureType, userId, promptVersion,
                    systemPrompt, userMessage, parameters);
        }
    }
}
