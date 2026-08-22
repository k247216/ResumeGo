package com.resumego.knowledge.dto;

/** 导入结果：重复 fingerprint 时 duplicate=true 且返回既有 document，不创建新记录。 */
public record KnowledgeImportResponse(
        Long documentId,
        String sourceType,
        String processingStatus,
        boolean duplicate,
        String errorCode
) {
}
