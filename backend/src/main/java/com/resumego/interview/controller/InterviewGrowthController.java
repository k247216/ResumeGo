package com.resumego.interview.controller;

import com.resumego.common.ApiResponse;
import com.resumego.interview.dto.InterviewGrowthReportResponse;
import com.resumego.interview.service.InterviewGrowthService;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 面试成长趋势查询接口。
 * 只读取已完成面试快照，不触发 AI 生成，也不控制面试状态。
 */
@RestController
@Validated
@RequiredArgsConstructor
public class InterviewGrowthController {

    private final InterviewGrowthService growthService;

    @GetMapping("/api/v1/interview-growth")
    public ResponseEntity<ApiResponse<InterviewGrowthReportResponse>> getGrowthReport(
            @RequestParam @Positive(message = "resumeId 必须为正整数") Long resumeId,
            @RequestParam @Positive(message = "jobDescriptionId 必须为正整数") Long jobDescriptionId) {
        return ResponseEntity.ok(ApiResponse.ok(growthService.getGrowthReport(resumeId, jobDescriptionId)));
    }
}
