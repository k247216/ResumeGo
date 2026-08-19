package com.resumego.interview.dto;

import java.time.LocalDateTime;

public record InterviewGrowthSnapshotResponse(
        Long resumeVersionId,
        String versionLabel,
        Long representativePlanId,
        LocalDateTime completedAt,
        int interviewCount,
        InterviewGrowthDimensions dimensions,
        String summary
) {
}
