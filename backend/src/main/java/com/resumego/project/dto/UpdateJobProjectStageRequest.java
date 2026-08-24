package com.resumego.project.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateJobProjectStageRequest(
        @NotBlank(message = "求职阶段不能为空")
        String stage
) {
}
