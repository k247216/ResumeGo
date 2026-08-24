package com.resumego.knowledge.dto;

/** 提取文本只通过该端点返回，不进入列表与日志。 */
public record KnowledgeContentResponse(
        Long documentId,
        String content
) {
}
