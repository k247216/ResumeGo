package com.resumego.knowledge.dto;

public record KnowledgeCategoryResponse(
        Long id,
        String name,
        String normalizedName,
        Long parentId,
        String createdAt,
        String updatedAt
) {
}
