package com.resumego.pipeline;

import java.time.LocalDateTime;

public record PipelineStageTransition(
        Long id,
        Long pipelineId,
        Long fromStageId,
        Long toStageId,
        String actor,
        String note,
        LocalDateTime occurredAt
) {
}
