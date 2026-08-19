package com.resumego.assessment.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumego.assessment.dto.ResumeAssessmentEvidenceRef;
import com.resumego.assessment.dto.ResumeAssessmentInput;
import com.resumego.assessment.dto.ResumeAssessmentResponse;
import com.resumego.assessment.dto.ResumeAssessmentResultDraft;
import com.resumego.assessment.repository.ResumeAssessmentRepository;
import com.resumego.resume.dto.ResumeVersionDTO;
import com.resumego.resume.service.ResumeService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ResumeAssessmentService {

    public static final String RULE_VERSION = "resume-score-v1";

    private final ResumeService resumeService;
    private final ResumeAssessmentRepository resumeAssessmentRepository;
    private final ResumeScoreCalculator resumeScoreCalculator;
    private final ObjectMapper objectMapper;

    public ResumeAssessmentService(
            ResumeService resumeService,
            ResumeAssessmentRepository resumeAssessmentRepository,
            ResumeScoreCalculator resumeScoreCalculator,
            ObjectMapper objectMapper
    ) {
        this.resumeService = resumeService;
        this.resumeAssessmentRepository = resumeAssessmentRepository;
        this.resumeScoreCalculator = resumeScoreCalculator;
        this.objectMapper = objectMapper;
    }

    public ResumeAssessmentResponse assess(long resumeVersionId) {
        ResumeVersionDTO version = resumeService.getVersion(resumeVersionId);
        List<ResumeAssessmentEvidenceRef> evidenceRefs = resumeAssessmentRepository.findEvidenceRefs(resumeVersionId);
        String inputFingerprint = fingerprint(version, evidenceRefs);

        return resumeAssessmentRepository
                .findByFingerprint(resumeVersionId, RULE_VERSION, inputFingerprint)
                .orElseGet(() -> createOrGetExisting(version, evidenceRefs, inputFingerprint));
    }

    private ResumeAssessmentResponse createOrGetExisting(
            ResumeVersionDTO version,
            List<ResumeAssessmentEvidenceRef> evidenceRefs,
            String inputFingerprint
    ) {
        try {
            return createAssessment(version, evidenceRefs, inputFingerprint);
        } catch (DuplicateKeyException e) {
            return resumeAssessmentRepository
                    .findByFingerprint(version.id(), RULE_VERSION, inputFingerprint)
                    .or(() -> resumeAssessmentRepository.findByVersionId(version.id()))
                    .orElseThrow(() -> new IllegalStateException("无法读取已有的简历评分记录", e));
        }
    }

    private ResumeAssessmentResponse createAssessment(
            ResumeVersionDTO version,
            List<ResumeAssessmentEvidenceRef> evidenceRefs,
            String inputFingerprint
    ) {
        ResumeAssessmentInput input = new ResumeAssessmentInput(version.id(), version.content(), evidenceRefs);
        ResumeAssessmentResultDraft result = resumeScoreCalculator.assess(input);
        long id = resumeAssessmentRepository.create(version.id(), RULE_VERSION, inputFingerprint, result);
        return resumeAssessmentRepository.findById(id)
                .orElseThrow(() -> new IllegalStateException("简历评分记录创建后无法读取"));
    }

    private String fingerprint(ResumeVersionDTO version, List<ResumeAssessmentEvidenceRef> evidenceRefs) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("ruleVersion", RULE_VERSION);
        payload.put("resumeVersionId", version.id());
        payload.put("content", version.content());
        payload.put("evidenceRefs", evidenceRefs);
        try {
            String json = objectMapper.writeValueAsString(payload);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("评分输入指纹生成失败：输入无法序列化", exception);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前运行环境不支持 SHA-256", exception);
        }
    }
}
