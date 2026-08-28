package com.resumego.knowledge;

/** 搜索原始行：包含正文供服务端计算 snippet/行号；响应中绝不返回全文。 */
public record KnowledgeSearchRow(
        long documentId,
        String title,
        String sourceType,
        String processingStatus,
        String sourceFile,
        Long sizeBytes,
        String createdAt,
        String updatedAt,
        String matchedField,
        String content
) {
    /** 兼容只返回来源文件名的旧搜索测试与调用方。 */
    public KnowledgeSearchRow(long documentId, String title, String sourceType, String processingStatus,
                              String sourceFile, String createdAt, String updatedAt,
                              String matchedField, String content) {
        this(documentId, title, sourceType, processingStatus, sourceFile, null,
                createdAt, updatedAt, matchedField, content);
    }
}
