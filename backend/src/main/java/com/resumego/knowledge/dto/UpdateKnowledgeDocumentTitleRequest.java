package com.resumego.knowledge.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/** 更新文档标题：trim 1-120；owner-scoped 更新，失败保留旧值。 */
public record UpdateKnowledgeDocumentTitleRequest(
        @NotBlank(message = "标题不能为空") @JsonProperty(required = true) String title
) {
}
