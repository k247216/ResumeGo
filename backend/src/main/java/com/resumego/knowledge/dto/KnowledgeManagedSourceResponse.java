package com.resumego.knowledge.dto;

/** 内部端点返回的受管相对路径：只返回 main 进程，绝不进入 renderer。 */
public record KnowledgeManagedSourceResponse(
        String relativePath
) {
}
