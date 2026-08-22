package com.resumego.knowledge.dto;

public record KnowledgeCategoryResponse(
        Long id,
        String name,
        String normalizedName,
        String createdAt,
        String updatedAt
) {
}
