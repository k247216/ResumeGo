package com.resumego.matching.controller;

import com.resumego.matching.dto.MatchRequest;
import com.resumego.matching.dto.MatchResponse;
import com.resumego.matching.service.MatchingPipelineService;
import com.resumego.matching.service.MatchingPipelineService.BatchMatchResult;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 岗位匹配接口。
 *
 * <pre>
 *   POST /api/resume-versions/{versionId}/job-matches   创建匹配
 *   GET  /api/job-matches/{matchId}                     查询已有匹配
 * </pre>
 */
@RestController
public class MatchingController {

    private final MatchingPipelineService matchingPipelineService;

    public MatchingController(MatchingPipelineService matchingPipelineService) {
        this.matchingPipelineService = matchingPipelineService;
    }

    /**
     * 执行并返回岗位匹配结果。
     * 相同输入重复请求返回已有结果（幂等）。
     */
    @PostMapping("/api/resume-versions/{versionId}/job-matches")
    public ResponseEntity<?> createMatch(
            @PathVariable Long versionId,
            @Valid @RequestBody MatchRequest request) {
        try {
            MatchResponse result = matchingPipelineService.match(versionId, request.jobDescriptionId());
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            if ("JD_NOT_FOUND".equals(e.getMessage())) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "JOB_DESCRIPTION_NOT_FOUND",
                                     "message", "JD 不存在或不属于当前用户"));
            }
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "RESUME_VERSION_NOT_FOUND",
                                 "message", "简历版本不存在或不属于当前用户"));
        } catch (IllegalStateException e) {
            if ("JD_NOT_PARSED".equals(e.getMessage())) {
                return ResponseEntity.status(422)
                        .body(Map.of("error", "JD_NOT_PARSED",
                                     "message", "JD 尚未完成结构化解析，请先调用 parse 接口"));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "INTERNAL_ERROR",
                                 "message", e.getMessage()));
        }
    }

    /**
     * 将一份简历与所有已解析 JD 匹配，返回匹配度最高的 topN 个结果。
     * 默认 topN=5。
     */
    @PostMapping("/api/resume-versions/{versionId}/batch-matches")
    public ResponseEntity<?> batchMatch(
            @PathVariable Long versionId,
            @RequestBody(required = false) Map<String, Integer> body) {
        try {
            int topN = body != null ? body.getOrDefault("topN", 5) : 5;
            topN = Math.max(1, Math.min(topN, 50)); // 限制 1-50
            List<BatchMatchResult> results = matchingPipelineService.batchMatch(versionId);
            int totalMatched = results.size();
            List<BatchMatchResult> topResults = totalMatched > topN
                    ? results.subList(0, topN) : results;
            return ResponseEntity.ok(Map.of(
                    "matches", topResults,
                    "totalCompared", totalMatched
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "RESUME_VERSION_NOT_FOUND",
                                 "message", e.getMessage()));
        }
    }

    /**
     * 查询已有匹配记录。
     */
    @GetMapping("/api/job-matches/{matchId}")
    public ResponseEntity<?> getMatch(@PathVariable Long matchId) {
        // TODO: 实现历史匹配查询（非 Sprint 1 核心路径）
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(Map.of("error", "NOT_IMPLEMENTED",
                             "message", "GET /api/job-matches/{matchId} 待实现"));
    }
}
