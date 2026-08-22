package com.resumego.knowledge;

/** 搜索原始行：包含正文供服务端计算 snippet/行号；响应中绝不返回全文。 */
public record KnowledgeSearchRow(
        long documentId,
        String title,
        String sourceType,
        String processingStatus,
        String sourceFile,
        String createdAt,
        String updatedAt,
        String matchedField,
        String content
) {
}
