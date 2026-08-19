package com.resumego.assessment.controller;

import com.resumego.assessment.dto.ResumeAssessmentResponse;
import com.resumego.assessment.service.ResumeAssessmentService;
import com.resumego.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/resume-versions")
public class ResumeAssessmentController {

    private final ResumeAssessmentService resumeAssessmentService;

    public ResumeAssessmentController(ResumeAssessmentService resumeAssessmentService) {
        this.resumeAssessmentService = resumeAssessmentService;
    }

    @PostMapping("/{versionId}/assessments")
    public ResponseEntity<ApiResponse<ResumeAssessmentResponse>> assess(@PathVariable Long versionId) {
        return ResponseEntity.ok(ApiResponse.ok(resumeAssessmentService.assess(versionId)));
    }
}
