package com.resumego.matching.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * POST /api/resume-versions/{versionId}/job-matches 响应体。
 * 扁平结构：返回前端展示和后续 AI 建议串联所需字段。
 */
@JsonPropertyOrder({"id", "matchScore", "details"})
public record MatchResponse(
        @JsonProperty("id")
        Long id,

        @JsonProperty("matchScore")
        int matchScore,

        @JsonProperty("details")
        MatchDetails details
) {
    public static MatchResponse of(Long id, int score, MatchDetails details) {
        return new MatchResponse(id, score, details);
    }
}
