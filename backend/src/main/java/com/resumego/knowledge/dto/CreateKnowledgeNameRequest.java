package com.resumego.knowledge.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateKnowledgeNameRequest(
        @NotBlank(message = "名称不能为空") String name
) {
}
