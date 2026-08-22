package com.resumego.pipeline.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.util.List;

public record ReorderPipelineStagesRequest(
        @NotEmpty List<@Valid @Positive Long> stageIds
) {
}
