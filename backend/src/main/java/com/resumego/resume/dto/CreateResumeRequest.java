package com.resumego.resume.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record CreateResumeRequest(
        @NotBlank(message = "简历名称不能为空")
        String title,

        @NotNull(message = "简历内容不能为空")
        Map<String, Object> content,

        String changeSummary,

        Long targetJobDescriptionId
) {
}
