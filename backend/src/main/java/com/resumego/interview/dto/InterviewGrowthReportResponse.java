package com.resumego.interview.dto;

import java.util.List;

public record InterviewGrowthReportResponse(
        Long resumeId,
        Long jobDescriptionId,
        String jobTitle,
        String companyName,
        List<InterviewGrowthSnapshotResponse> snapshots,
        InterviewGrowthDimensions changes
) {
}
