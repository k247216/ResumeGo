package com.resumego.ai.provider;

import java.time.LocalDateTime;

public record AiProviderProfileResponse(
        Long id,
        String displayName,
        String protocolType,
        String baseUrl,
        String defaultModel,
        boolean defaultProfile,
        boolean apiKeyConfigured,
        LocalDateTime lastTestedAt,
        String lastTestStatus,
        String lastTestMessage
) {
}
