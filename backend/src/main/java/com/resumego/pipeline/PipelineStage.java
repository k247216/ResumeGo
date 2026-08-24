package com.resumego.pipeline;

import java.time.LocalDateTime;

public record PipelineStage(
        Long id,
        Long pipelineId,
        String name,
        Integer position,
        PipelineStageState state,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
