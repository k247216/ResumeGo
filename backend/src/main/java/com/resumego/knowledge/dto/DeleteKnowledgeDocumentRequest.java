package com.resumego.knowledge.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record DeleteKnowledgeDocumentRequest(
        @NotBlank(message = "确认令牌不能为空") @JsonProperty(required = true) String confirmationToken
) {
}
