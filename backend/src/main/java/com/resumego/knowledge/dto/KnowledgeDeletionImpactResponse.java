package com.resumego.knowledge.dto;

import java.time.LocalDateTime;

/** 删除影响摘要：不返回路径/hash/正文；confirmationToken 为一次性明文。 */
public record KnowledgeDeletionImpactResponse(
        String title,
        boolean hasSource,
        boolean hasContent,
        boolean hasCategory,
        boolean hasTags,
        String confirmationToken,
        LocalDateTime expiresAt
) {
}
