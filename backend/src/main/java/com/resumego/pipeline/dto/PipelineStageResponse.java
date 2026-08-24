package com.resumego.pipeline.dto;

import com.resumego.pipeline.PipelineStageState;

public record PipelineStageResponse(
        Long id,
        String name,
        Integer position,
        PipelineStageState state
) {
}
