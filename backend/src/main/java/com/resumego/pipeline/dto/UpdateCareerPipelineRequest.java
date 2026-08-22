package com.resumego.pipeline.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

/**
 * Pipeline 全量更新请求。
 *
 * 冻结契约要求五个字段都必须出现：前三个为非空字符串，后两个可显式为
 * null 解除关联。@JsonProperty(required = true) 让 Jackson 在字段缺失时
 * 直接抛异常（映射为 400），从而与"显式 null"可区分，避免客户端漏字段
 * 时静默清除已绑定的 JD/简历。
 */
public record UpdateCareerPipelineRequest(
        @NotBlank
        @JsonProperty(required = true)
        String name,

        @NotBlank
        @JsonProperty(required = true)
        String companyName,

        @NotBlank
        @JsonProperty(required = true)
        String roleTitle,

        @JsonProperty(required = true)
        Long jobDescriptionId,

        @JsonProperty(required = true)
        Long resumeVersionId
) {
}
