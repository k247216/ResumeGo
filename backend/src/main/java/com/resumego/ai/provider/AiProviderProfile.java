package com.resumego.ai.provider;

import java.time.LocalDateTime;

public record AiProviderProfile(
        Long id,
        Long userId,
        String displayName,
        String protocolType,
        String baseUrl,
        String defaultModel,
        boolean defaultProfile,
        LocalDateTime lastTestedAt,
        String lastTestStatus,
        String lastTestMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
