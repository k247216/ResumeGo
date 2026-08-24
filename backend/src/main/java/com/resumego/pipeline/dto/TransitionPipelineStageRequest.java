package com.resumego.pipeline.dto;

import jakarta.validation.constraints.Positive;

public record TransitionPipelineStageRequest(
        @Positive long targetStageId,
        String note
) {
}
