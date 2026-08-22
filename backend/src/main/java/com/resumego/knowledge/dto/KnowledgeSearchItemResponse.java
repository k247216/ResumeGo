package com.resumego.knowledge.dto;

/** 搜索结果项：document 元数据 + 匹配字段 + 短 snippet + 正文 1-based 行号；不返回全文。 */
public record KnowledgeSearchItemResponse(
        KnowledgeDocumentResponse document,
        String matchedField,
        String snippet,
        Integer lineNumber
) {
}
