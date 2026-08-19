package com.resumego.resume.controller;

import com.resumego.common.ApiResponse;
import com.resumego.resume.dto.CapabilityEvidenceCreateRequest;
import com.resumego.resume.dto.CapabilityEvidenceResponse;
import com.resumego.resume.service.CapabilityEvidenceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/evidences")
public class CapabilityEvidenceController {

    private final CapabilityEvidenceService capabilityEvidenceService;

    public CapabilityEvidenceController(CapabilityEvidenceService capabilityEvidenceService) {
        this.capabilityEvidenceService = capabilityEvidenceService;
    }

    @GetMapping
    public ApiResponse<List<CapabilityEvidenceResponse>> list() {
        return ApiResponse.ok(capabilityEvidenceService.listDemoUserEvidences());
    }

    @GetMapping("/{id}")
    public ApiResponse<CapabilityEvidenceResponse> get(@PathVariable Long id) {
        return ApiResponse.ok(capabilityEvidenceService.getDemoUserEvidence(id));
    }

    @PostMapping
    public ApiResponse<CapabilityEvidenceResponse> create(
            @Valid @RequestBody CapabilityEvidenceCreateRequest request
    ) {
        return ApiResponse.ok(capabilityEvidenceService.createDemoUserEvidence(request));
    }
}
