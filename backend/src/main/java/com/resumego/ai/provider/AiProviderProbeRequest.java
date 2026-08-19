package com.resumego.ai.provider;

public record AiProviderProbeRequest(
        String displayName,
        String protocolType,
        String baseUrl,
        String defaultModel,
        String apiKey
) {
    public AiProviderProfileRequest profileRequest() {
        return new AiProviderProfileRequest(displayName, protocolType, baseUrl, defaultModel);
    }
}
