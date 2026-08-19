package com.resumego.matching.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * POST /api/resume-versions/{versionId}/job-matches 请求体。
 * versionId 从路径获取，请求体只需指定目标 JD。
 */
public record MatchRequest(
        @NotNull(message = "jobDescriptionId 不能为空")
        @Positive(message = "jobDescriptionId 必须为正整数")
        @JsonProperty("jobDescriptionId")
        Long jobDescriptionId
) {
}
