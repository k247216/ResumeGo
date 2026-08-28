package com.resumego.resume.dto;

import jakarta.validation.constraints.Size;

public record UpdateResumeVersionSummaryRequest(
        @Size(max = 240, message = "版本说明不能超过 240 个字符")
        String changeSummary
) {
}
