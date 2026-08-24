package com.resumego.knowledge.dto;

public record KnowledgeDocumentResponse(
        Long id,
        String title,
        String sourceType,
        String processingStatus,
        String sourceFile,
        String sourceExtension,
        String createdAt,
        String updatedAt
) {
}
