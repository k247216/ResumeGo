package com.resumego.optimization;

import com.resumego.common.ApiResponse;
import com.resumego.optimization.dto.GenerateSuggestionsRequest;
import com.resumego.optimization.dto.GenerateSuggestionsResponse;
import com.resumego.optimization.dto.SuggestionFollowUpRequest;
import com.resumego.optimization.dto.SuggestionFollowUpResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI 优化建议 Controller。
 * 提供基于匹配结果生成、查看、采纳和拒绝优化建议的 REST 接口。
 */
@RestController
@Validated
@RequiredArgsConstructor
public class OptimizationSuggestionController {

    private static final Logger log = LoggerFactory.getLogger(OptimizationSuggestionController.class);

    private final OptimizationSuggestionService suggestionService;
    private final SuggestionFollowUpService followUpService;

    /**
     * 生成 AI 优化建议。
     * POST /api/v1/job-matches/{matchId}/suggestions
     * <p>
     * 模式自动切换：API Key 已配置 → 使用真实匹配+评分接口；无 API Key → 使用匹配结果表中数据。
     */
    @PostMapping("/api/v1/job-matches/{matchId}/suggestions")
    public ResponseEntity<ApiResponse<GenerateSuggestionsResponse>> generateSuggestions(
            @PathVariable @Positive(message = "matchId 必须为正整数") Long matchId) {
        return generateSuggestionsByMatchId(matchId);
    }

    /**
     * 生成 AI 优化建议（通过简历版本 ID）。
     * POST /api/v1/resume-versions/{versionId}/ai-suggestions
     */
    @PostMapping("/api/v1/resume-versions/{versionId}/ai-suggestions")
    public ResponseEntity<ApiResponse<GenerateSuggestionsResponse>> generateSuggestionsByVersionId(
            @PathVariable @Positive(message = "versionId 必须为正整数") Long versionId,
            @Valid @RequestBody GenerateSuggestionsRequest request) {
        log.info("收到 AI 建议生成请求: versionId={}, jobDescriptionId={}, assessmentId={}, matchId={}",
                versionId, request.jobDescriptionId(), request.assessmentId(), request.matchId());
        return generateSuggestionsByMatchId(request.matchId());
    }

    private ResponseEntity<ApiResponse<GenerateSuggestionsResponse>> generateSuggestionsByMatchId(Long matchId) {
        try {
            GenerateSuggestionsResponse result = suggestionService.generateSuggestions(matchId);
            return ResponseEntity.ok(ApiResponse.ok(result));
        } catch (IllegalArgumentException e) {
            log.warn("生成建议参数校验失败: matchId={}, message={}", matchId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.fail("请求参数无效，请检查输入"));
        } catch (IllegalStateException e) {
            log.warn("生成建议状态校验失败: matchId={}, message={}", matchId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.fail("当前状态不允许此操作"));
        } catch (Exception e) {
            log.error("生成建议时发生未预期错误: matchId={}", matchId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("服务内部错误，请稍后重试"));
        }
    }

    /**
     * 获取已生成的建议列表。
     * GET /api/v1/job-matches/{matchId}/suggestions
     */
    @GetMapping("/api/v1/job-matches/{matchId}/suggestions")
    public ResponseEntity<ApiResponse<GenerateSuggestionsResponse>> getSuggestions(
            @PathVariable @Positive(message = "matchId 必须为正整数") Long matchId) {
        try {
            GenerateSuggestionsResponse result = suggestionService.getSuggestions(matchId);
            return ResponseEntity.ok(ApiResponse.ok(result));
        } catch (Exception e) {
            log.error("查询建议时发生未预期错误: matchId={}", matchId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("服务内部错误，请稍后重试"));
        }
    }

    /**
     * 基于用户补充事实生成最终建议。
     * POST /api/v1/suggestions/{suggestionId}/follow-up
     * <p>
     * 该接口只返回建议，不修改简历、不更新建议状态、不创建新版本。
     */
    @PostMapping("/api/v1/suggestions/{suggestionId}/follow-up")
    public ResponseEntity<ApiResponse<SuggestionFollowUpResponse>> generateFollowUpAdvice(
            @PathVariable @Positive(message = "suggestionId 必须为正整数") Long suggestionId,
            @Valid @RequestBody SuggestionFollowUpRequest request) {
        try {
            SuggestionFollowUpResponse result = followUpService.generateFinalAdvice(suggestionId, request);
            return ResponseEntity.ok(ApiResponse.ok(result));
        } catch (IllegalArgumentException e) {
            log.warn("生成追问最终建议参数校验失败: suggestionId={}, message={}", suggestionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.fail(e.getMessage()));
        } catch (Exception e) {
            log.error("生成追问最终建议时发生未预期错误: suggestionId={}", suggestionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("服务内部错误，请稍后重试"));
        }
    }

    /**
     * 采纳建议。
     * POST /api/v1/suggestions/{suggestionId}/accept
     */
    @PostMapping("/api/v1/suggestions/{suggestionId}/accept")
    public ResponseEntity<ApiResponse<Void>> acceptSuggestion(
            @PathVariable @Positive(message = "suggestionId 必须为正整数") Long suggestionId) {
        try {
            suggestionService.acceptSuggestion(suggestionId);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (IllegalArgumentException e) {
            log.warn("采纳建议参数校验失败: suggestionId={}, message={}", suggestionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.fail("请求参数无效，请检查输入"));
        } catch (IllegalStateException e) {
            log.warn("采纳建议状态校验失败: suggestionId={}, message={}", suggestionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.fail("当前状态不允许此操作"));
        } catch (Exception e) {
            log.error("采纳建议时发生未预期错误: suggestionId={}", suggestionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("服务内部错误，请稍后重试"));
        }
    }

    /**
     * 拒绝建议。
     * POST /api/v1/suggestions/{suggestionId}/reject
     */
    @PostMapping("/api/v1/suggestions/{suggestionId}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectSuggestion(
            @PathVariable @Positive(message = "suggestionId 必须为正整数") Long suggestionId) {
        try {
            suggestionService.rejectSuggestion(suggestionId);
            return ResponseEntity.ok(ApiResponse.ok(null));
        } catch (IllegalArgumentException e) {
            log.warn("拒绝建议参数校验失败: suggestionId={}, message={}", suggestionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.fail("请求参数无效，请检查输入"));
        } catch (IllegalStateException e) {
            log.warn("拒绝建议状态校验失败: suggestionId={}, message={}", suggestionId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.fail("当前状态不允许此操作"));
        } catch (Exception e) {
            log.error("拒绝建议时发生未预期错误: suggestionId={}", suggestionId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.fail("服务内部错误，请稍后重试"));
        }
    }
}
