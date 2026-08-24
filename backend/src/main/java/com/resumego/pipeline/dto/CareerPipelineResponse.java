package com.resumego.pipeline.dto;

import com.resumego.pipeline.PipelineLifecycle;
import com.resumego.pipeline.PipelineOutcome;

import java.time.LocalDateTime;
import java.util.List;

public record CareerPipelineResponse(
        Long id,
        String name,
        String companyName,
        String roleTitle,
        Long jobDescriptionId,
        Long resumeVersionId,
        PipelineLifecycle lifecycle,
        PipelineOutcome outcome,
        Long currentStageId,
        List<PipelineStageResponse> stages,
        List<Long> scheduleEventIds,
        List<Long> interviewPlanIds,
        LocalDateTime archivedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
