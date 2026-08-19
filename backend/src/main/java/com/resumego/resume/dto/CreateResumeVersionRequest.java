package com.resumego.resume.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record CreateResumeVersionRequest(
        @NotNull(message = "简历内容不能为空")
        Map<String, Object> content,
        String changeSummary
) {
}
