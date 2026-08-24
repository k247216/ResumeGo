package com.resumego.pipeline.dto;

import java.time.LocalDateTime;

public record PipelineStageTransitionResponse(
        Long id,
        Long pipelineId,
        Long fromStageId,
        Long toStageId,
        String actor,
        String note,
        LocalDateTime occurredAt
) {
}
