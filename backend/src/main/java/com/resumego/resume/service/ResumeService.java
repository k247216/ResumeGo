package com.resumego.resume.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumego.common.CurrentUser;
import com.resumego.resume.dto.CreateResumeRequest;
import com.resumego.resume.dto.CreateResumeVersionRequest;
import com.resumego.resume.dto.ResumeDTO;
import com.resumego.resume.dto.ResumeVersionDTO;
import com.resumego.resume.dto.UpdateResumeTargetJobRequest;
import com.resumego.resume.repository.ResumeRepository;
import com.resumego.resume.repository.ResumeRepository.EvidenceRefDraft;
import com.resumego.resume.repository.ResumeRepository.ProjectEvidenceDraft;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
public class ResumeService {

    /** 简历内容最大长度（字符数），防止超长文本导致 AI Token 超额 */
    private static final int MAX_RESUME_CONTENT_LENGTH = 50_000;

    private final ResumeRepository resumeRepository;
    private final ObjectMapper objectMapper;

    public ResumeService(ResumeRepository resumeRepository, ObjectMapper objectMapper) {
        this.resumeRepository = resumeRepository;
        this.objectMapper = objectMapper;
    }

    public List<ResumeDTO> listByDemoUser() {
        List<Long> resumeIds = resumeRepository.findIdsByUserId(CurrentUser.DEMO_USER_ID);
        List<ResumeDTO> result = new ArrayList<>();
        for (Long id : resumeIds) {
            result.add(buildResumeResponse(id));
        }
        return result;
    }

    @Transactional
    public ResumeDTO createResume(CreateResumeRequest request) {
        if (request == null || request.content() == null || request.content().isEmpty()) {
            throw new IllegalArgumentException("简历内容不能为空");
        }
        String title = request.title() == null ? "" : request.title().trim();
        if (title.isBlank()) {
            throw new IllegalArgumentException("简历名称不能为空");
        }

        Map<String, Object> content = prepareContentWithProjectEvidence(request.content());
        String contentJson;
        try {
            contentJson = objectMapper.writeValueAsString(content);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("简历内容格式不合法", e);
        }

        String changeSummary = request.changeSummary();
        if (changeSummary == null || changeSummary.isBlank()) {
            changeSummary = "创建新简历";
        }

        long resumeId = resumeRepository.insertResume(CurrentUser.DEMO_USER_ID, title, request.targetJobDescriptionId());
        long versionId = resumeRepository.insertVersion(
                resumeId,
                null,
                1,
                contentJson,
                changeSummary,
                "user"
        );
        resumeRepository.replaceEvidenceRefsForVersion(
                versionId,
                CurrentUser.DEMO_USER_ID,
                extractProjectEvidenceRefs(content)
        );
        resumeRepository.updateCurrentVersionId(resumeId, versionId);
        return buildResumeResponse(resumeId);
    }

    @Transactional
    public ResumeDTO updateTargetJob(long resumeId, UpdateResumeTargetJobRequest request) {
        String title = resumeRepository.findTitleById(resumeId);
        if (title == null) {
            throw new IllegalArgumentException("简历不存在");
        }

        resumeRepository.updateTargetJobDescriptionId(
                resumeId,
                request == null ? null : request.targetJobDescriptionId()
        );
        return buildResumeResponse(resumeId);
    }

    private ResumeDTO buildResumeResponse(long resumeId) {
        String title = resumeRepository.findTitleById(resumeId);
        Long targetJobDescriptionId = resumeRepository.findTargetJobDescriptionIdById(resumeId);
        Long versionId = resumeRepository.findCurrentVersionId(resumeId);
        ResumeVersionDTO currentVersion = null;
        if (versionId != null) {
            currentVersion = resumeRepository.findVersionById(versionId);
        }
        return new ResumeDTO(resumeId, title, targetJobDescriptionId, currentVersion, null, null);
    }

    public ResumeVersionDTO getVersion(long versionId) {
        ResumeVersionDTO version = resumeRepository.findVersionById(versionId);
        if (version == null) {
            throw new IllegalArgumentException("简历版本不存在");
        }
        return version;
    }

    public List<ResumeVersionDTO> getVersions(long resumeId) {
        String title = resumeRepository.findTitleById(resumeId);
        if (title == null) {
            throw new IllegalArgumentException("简历不存在");
        }
        return resumeRepository.findVersionsByResumeId(resumeId);
    }

    @Transactional
    public ResumeVersionDTO createManualVersion(long resumeId, CreateResumeVersionRequest request) {
        if (request == null || request.content() == null || request.content().isEmpty()) {
            throw new IllegalArgumentException("简历内容不能为空");
        }

        String title = resumeRepository.findTitleById(resumeId);
        if (title == null) {
            throw new IllegalArgumentException("简历不存在");
        }

        Long parentVersionId = resumeRepository.findCurrentVersionId(resumeId);
        int newVersionNo = resumeRepository.findMaxVersionNo(resumeId) + 1;
        Map<String, Object> content = prepareContentWithProjectEvidence(request.content());
        String contentJson;
        try {
            contentJson = objectMapper.writeValueAsString(content);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("简历内容格式不合法", e);
        }

        // 校验：纯空白内容（如全空格字符串）视为空内容
        if (contentJson.isBlank()) {
            throw new IllegalArgumentException("简历内容不能为空");
        }

        // 校验：内容长度超限
        if (contentJson.length() > MAX_RESUME_CONTENT_LENGTH) {
            throw new IllegalArgumentException(
                    "简历内容过长，当前 " + contentJson.length() + " 字符，最大允许 " + MAX_RESUME_CONTENT_LENGTH + " 字符");
        }

        String changeSummary = request.changeSummary();
        if (changeSummary == null || changeSummary.isBlank()) {
            changeSummary = "人工编辑生成新版本";
        }

        long newVersionId = resumeRepository.insertVersion(
                resumeId,
                parentVersionId,
                newVersionNo,
                contentJson,
                changeSummary,
                "user"
        );
        resumeRepository.replaceEvidenceRefsForVersion(
                newVersionId,
                CurrentUser.DEMO_USER_ID,
                extractProjectEvidenceRefs(content)
        );
        resumeRepository.updateCurrentVersionId(resumeId, newVersionId);

        return resumeRepository.findVersionById(newVersionId);
    }

    private Map<String, Object> prepareContentWithProjectEvidence(Map<String, Object> rawContent) {
        Map<String, Object> content = objectMapper.convertValue(rawContent, new TypeReference<>() {});
        Object projectsValue = content.get("projects");
        if (!(projectsValue instanceof List<?> projects)) {
            return content;
        }
        for (Object item : projects) {
            if (!(item instanceof Map<?, ?> rawProject)) {
                continue;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> project = (Map<String, Object>) rawProject;
            Long evidenceId = toPositiveLong(project.get("evidenceId"));
            if (evidenceId != null
                    && resumeRepository.existsActiveEvidenceForUser(CurrentUser.DEMO_USER_ID, evidenceId)) {
                continue;
            }
            if (!hasProjectEvidenceContent(project)) {
                continue;
            }
            ProjectEvidenceDraft evidence = buildSelfReportedProjectEvidence(project);
            long generatedEvidenceId = resumeRepository.createSelfReportedProjectEvidence(
                    CurrentUser.DEMO_USER_ID,
                    evidence
            );
            project.put("evidenceId", generatedEvidenceId);
        }
        return content;
    }

    private boolean hasProjectEvidenceContent(Map<String, Object> project) {
        return !firstText(project, "title", "name", "projectName").isBlank()
                || !firstText(project, "description", "responsibility", "responsibilities",
                "summary", "detail", "content", "actionText").isBlank()
                || !extractSkillTags(project).isEmpty();
    }

    private ProjectEvidenceDraft buildSelfReportedProjectEvidence(Map<String, Object> project) {
        String title = firstText(project, "title", "name", "projectName");
        if (title.isBlank()) {
            title = "未命名项目经历";
        }
        String actionText = firstText(project, "description", "responsibility", "responsibilities",
                "summary", "detail", "content", "actionText");
        if (actionText.isBlank()) {
            actionText = title;
        }
        String resultText = firstText(project, "result", "resultText", "impact", "achievement", "achievements");
        List<String> skillTags = extractSkillTags(project);
        if (skillTags.isEmpty()) {
            skillTags = List.of("项目经历");
        }
        return new ProjectEvidenceDraft(
                title,
                "来源：简历项目经历「" + title + "」",
                actionText,
                resultText.isBlank() ? null : resultText,
                skillTags,
                "系统根据简历项目经历自动生成的自述型证据"
        );
    }

    private String firstText(Map<String, Object> source, String... keys) {
        for (String key : keys) {
            Object value = source.get(key);
            if (value instanceof String text && !text.isBlank()) {
                return text.trim();
            }
        }
        return "";
    }

    private List<String> extractSkillTags(Map<String, Object> project) {
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        collectSkillTags(tags, project.get("techStack"));
        collectSkillTags(tags, project.get("technologies"));
        collectSkillTags(tags, project.get("skills"));
        collectSkillTags(tags, project.get("skillTags"));
        return List.copyOf(tags);
    }

    private void collectSkillTags(LinkedHashSet<String> tags, Object value) {
        if (value instanceof Collection<?> values) {
            for (Object item : values) {
                if (item instanceof String text && !text.isBlank()) {
                    tags.add(text.trim());
                } else {
                    collectSkillTags(tags, item);
                }
            }
            return;
        }
        if (value instanceof String text) {
            for (String part : text.split("[,，/、;；]+")) {
                String tag = part.trim();
                if (!tag.isBlank()) {
                    tags.add(tag);
                }
            }
        }
    }

    private List<EvidenceRefDraft> extractProjectEvidenceRefs(Map<String, Object> content) {
        Object projectsValue = content.get("projects");
        if (!(projectsValue instanceof List<?> projects)) {
            return List.of();
        }
        List<EvidenceRefDraft> refs = new ArrayList<>();
        for (int index = 0; index < projects.size(); index++) {
            Object item = projects.get(index);
            if (!(item instanceof Map<?, ?> project)) {
                continue;
            }
            Long evidenceId = toPositiveLong(project.get("evidenceId"));
            if (evidenceId == null) {
                continue;
            }
            refs.add(new EvidenceRefDraft(evidenceId, "projects[" + index + "]"));
        }
        return refs;
    }

    private Long toPositiveLong(Object value) {
        if (value instanceof Number number) {
            long parsed = number.longValue();
            return parsed > 0 ? parsed : null;
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                long parsed = Long.parseLong(text.trim());
                return parsed > 0 ? parsed : null;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    @Transactional
    public boolean deleteResume(long resumeId) {
        return resumeRepository.softDelete(CurrentUser.DEMO_USER_ID, resumeId) > 0;
    }
}
