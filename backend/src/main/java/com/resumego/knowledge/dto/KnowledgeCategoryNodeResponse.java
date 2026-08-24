package com.resumego.knowledge.dto;

/** 层级分类列表节点：前端构树；计数由真实关联计算。 */
public record KnowledgeCategoryNodeResponse(
        Long id,
        String name,
        String normalizedName,
        Long parentId,
        int depth,
        int directDocumentCount,
        int descendantDocumentCount,
        String createdAt,
        String updatedAt
) {
}
