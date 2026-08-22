package com.resumego.knowledge.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record CreateKnowledgeDocumentRequest(
        @NotBlank @JsonProperty(required = true) String title,
        @NotBlank @JsonProperty(required = true) String sourceType
) {
}
