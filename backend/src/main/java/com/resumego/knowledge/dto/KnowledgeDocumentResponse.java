package com.resumego.knowledge.dto;

public record KnowledgeDocumentResponse(
        Long id,
        String title,
        String sourceType,
        String processingStatus,
        String sourceFile,
        String sourceExtension,
        Long sizeBytes,
        String createdAt,
        String updatedAt
) {
    /** 兼容旧调用方：历史构造不提供文件大小时保持 null。 */
    public KnowledgeDocumentResponse(Long id, String title, String sourceType, String processingStatus,
                                     String sourceFile, String sourceExtension, String createdAt, String updatedAt) {
        this(id, title, sourceType, processingStatus, sourceFile, sourceExtension, null, createdAt, updatedAt);
    }
}
