package com.resumego.knowledge.dto;

import jakarta.validation.constraints.NotBlank;

/** 创建分类：parentId 可为 null（根节点）；旧 {name} 等价根节点。 */
public record CreateKnowledgeCategoryRequest(
        @NotBlank(message = "名称不能为空") String name,
        Long parentId
) {
    /** 旧 {name} 请求等价根节点。 */
    public CreateKnowledgeCategoryRequest(String name) {
        this(name, null);
    }
}
