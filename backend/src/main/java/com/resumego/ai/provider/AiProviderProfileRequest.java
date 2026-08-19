package com.resumego.ai.provider;

public record AiProviderProfileRequest(
        String displayName,
        String protocolType,
        String baseUrl,
        String defaultModel
) {
}
