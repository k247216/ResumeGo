package com.resumego.knowledge.dto;

public record KnowledgeTagResponse(
        Long id,
        String name,
        String normalizedName,
        String createdAt,
        String updatedAt
) {
}
