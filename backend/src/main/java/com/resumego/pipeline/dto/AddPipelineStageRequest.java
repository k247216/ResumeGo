package com.resumego.pipeline.dto;

import jakarta.validation.constraints.NotBlank;

public record AddPipelineStageRequest(@NotBlank String name) {
}
