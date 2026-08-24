package com.resumego.resume.dto;

import jakarta.validation.constraints.NotBlank;

/** 简历资产改名请求。 */
public record UpdateResumeAssetRequest(
        @NotBlank(message = "简历名称不能为空") String title
) {
}
