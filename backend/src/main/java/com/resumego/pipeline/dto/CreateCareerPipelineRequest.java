package com.resumego.pipeline.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record CreateCareerPipelineRequest(
        @NotBlank String name,
        @NotBlank String companyName,
        @NotBlank String roleTitle,
        Long jobDescriptionId,
        Long resumeVersionId,
        List<String> stages
) {
}
