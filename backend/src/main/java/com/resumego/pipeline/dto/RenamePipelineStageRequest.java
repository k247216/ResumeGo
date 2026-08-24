package com.resumego.pipeline.dto;

import jakarta.validation.constraints.NotBlank;

public record RenamePipelineStageRequest(@NotBlank String name) {
}
