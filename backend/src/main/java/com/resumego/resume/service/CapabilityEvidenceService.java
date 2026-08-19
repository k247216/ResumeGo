package com.resumego.resume.service;

import com.resumego.common.CurrentUser;
import com.resumego.resume.dto.CapabilityEvidenceCreateRequest;
import com.resumego.resume.dto.CapabilityEvidenceResponse;
import com.resumego.resume.repository.CapabilityEvidenceRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class CapabilityEvidenceService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "project",
            "internship",
            "competition",
            "skill",
            "other"
    );

    private final CapabilityEvidenceRepository capabilityEvidenceRepository;

    public CapabilityEvidenceService(CapabilityEvidenceRepository capabilityEvidenceRepository) {
        this.capabilityEvidenceRepository = capabilityEvidenceRepository;
    }

    public List<CapabilityEvidenceResponse> listDemoUserEvidences() {
        return capabilityEvidenceRepository.findActiveByUserId(CurrentUser.DEMO_USER_ID);
    }

    public CapabilityEvidenceResponse getDemoUserEvidence(Long id) {
        return capabilityEvidenceRepository.findActiveByIdAndUserId(id, CurrentUser.DEMO_USER_ID)
                .orElseThrow(() -> new IllegalArgumentException("能力证据不存在或无权访问"));
    }

    public CapabilityEvidenceResponse createDemoUserEvidence(CapabilityEvidenceCreateRequest request) {
        validateEvidenceType(request.evidenceType());
        long id = capabilityEvidenceRepository.create(CurrentUser.DEMO_USER_ID, request);
        return getDemoUserEvidence(id);
    }

    private void validateEvidenceType(String evidenceType) {
        if (!ALLOWED_TYPES.contains(evidenceType)) {
            throw new IllegalArgumentException("经历类型仅支持 project/internship/competition/skill/other");
        }
    }
}
