package com.resumego.knowledge.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/** 更新分类：name 与 parentId 必须显式出现（parentId 可为 null 表示移到根）。 */
public record UpdateKnowledgeCategoryRequest(
        @NotBlank(message = "名称不能为空") @JsonProperty(required = true) String name,
        @JsonProperty(required = true) Long parentId
) {
}
