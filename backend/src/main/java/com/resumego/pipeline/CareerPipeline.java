package com.resumego.pipeline;

import java.time.LocalDateTime;

public record CareerPipeline(
        Long id,
        Long userId,
        String name,
        String companyName,
        String roleTitle,
        Long jobDescriptionId,
        Long resumeVersionId,
        PipelineLifecycle lifecycle,
        PipelineOutcome outcome,
        Long currentStageId,
        LocalDateTime archivedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
