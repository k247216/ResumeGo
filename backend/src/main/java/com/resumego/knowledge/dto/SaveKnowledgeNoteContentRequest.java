package com.resumego.knowledge.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;

/** 保存 NOTE 正文：content 允许空字符串，最大 1 MiB UTF-8 bytes；不 trim、不重写。 */
public record SaveKnowledgeNoteContentRequest(
        @NotNull(message = "正文不能为空值") @JsonProperty(required = true) String content
) {
}
