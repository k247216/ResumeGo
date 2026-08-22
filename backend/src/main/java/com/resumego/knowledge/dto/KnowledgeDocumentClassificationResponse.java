package com.resumego.knowledge.dto;

import java.util.List;

/** 文档现有关联：category 可为 null，tags 为空数组；不塞进 KnowledgeDocumentResponse。 */
public record KnowledgeDocumentClassificationResponse(
        KnowledgeCategoryResponse category,
        List<KnowledgeTagResponse> tags
) {
}
