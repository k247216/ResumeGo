package com.resumego.pipeline.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateCareerPipelineRequest(
        @NotBlank String name,
        @NotBlank String companyName,
        @NotBlank String roleTitle,
        Long jobDescriptionId,
        Long resumeVersionId
) {
}
