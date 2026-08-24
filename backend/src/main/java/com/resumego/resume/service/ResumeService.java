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
import java.util.NoSuchElementException;

@Service
public class ResumeService {

    /** 简历内容最大长度（字符数），防止超长文本导致 AI Token 超额 */
    private static final int MAX_RESUME_CONTENT_LENGTH = 50_000;

    /** 简历资产种类（冻结契约：只有 GENERAL 与 JOB_EXPRESSION） */
    public static final String KIND_GENERAL = "GENERAL";
    public static final String KIND_JOB_EXPRESSION = "JOB_EXPRESSION";

    private static final int MAX_TITLE_LENGTH = 120;

    private final ResumeRepository resumeRepository;
    private final ObjectMapper objectMapper;

    public ResumeService(ResumeRepository resumeRepository, ObjectMapper objectMapper) {
        this.resumeRepository = resumeRepository;
        this.objectMapper = objectMapper;
    }

    public List<ResumeDTO> listByDemoUser() {
        return listAssets(null, false);
    }

    /** 资产列表：kind 过滤（null=全部），archived 缺省 false。 */
    public List<ResumeDTO> listAssets(String kind, Boolean archived) {
        if (kind != null && !KIND_GENERAL.equals(kind) && !KIND_JOB_EXPRESSION.equals(kind)) {
            throw new IllegalArgumentException("未知的简历种类");
        }
        boolean archivedFlag = archived != null && archived;
        List<Long> resumeIds = resumeRepository.findIdsByUserId(CurrentUser.DEMO_USER_ID, kind, archivedFlag);
        List<ResumeDTO> result = new ArrayList<>();
        for (Long id : resumeIds) {
            result.add(buildResumeResponse(CurrentUser.DEMO_USER_ID, id));
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
        return buildResumeResponse(CurrentUser.DEMO_USER_ID, resumeId);
    }

    @Transactional
    public ResumeDTO updateTargetJob(long resumeId, UpdateResumeTargetJobRequest request) {
        String title = resumeRepository.findTitleById(CurrentUser.DEMO_USER_ID, resumeId);
        if (title == null) {
            throw new IllegalArgumentException("简历不存在");
        }

        resumeRepository.updateTargetJobDescriptionId(
                resumeId,
                request == null ? null : request.targetJobDescriptionId()
        );
        return buildResumeResponse(CurrentUser.DEMO_USER_ID, resumeId);
    }

    /** 创建岗位表达副本：服务端读取源版本正文复制，renderer 只提交新标题。 */
    @Transactional
    public ResumeDTO forkVersion(long sourceVersionId, com.resumego.resume.dto.ForkResumeVersionRequest request) {
        String title = normalizeTitle(request == null ? null : request.title());

        ResumeVersionDTO source = resumeRepository.findVersionByIdForUser(CurrentUser.DEMO_USER_ID, sourceVersionId);
        if (source == null) {
            throw new NoSuchElementException("简历版本不存在");
        }
        String contentJson = resumeRepository.findContentJsonById(sourceVersionId);
        if (contentJson == null || contentJson.isBlank()) {
            throw new IllegalArgumentException("源版本内容为空，无法创建副本");
        }

        long newResumeId = resumeRepository.createForkedAsset(
                CurrentUser.DEMO_USER_ID, title, sourceVersionId, source.versionNo(), contentJson);
        return buildResumeResponse(CurrentUser.DEMO_USER_ID, newResumeId);
    }

    /** 简历资产改名。 */
    @Transactional
    public ResumeDTO renameResume(long resumeId, com.resumego.resume.dto.UpdateResumeAssetRequest request) {
        String title = normalizeTitle(request == null ? null : request.title());
        if (resumeRepository.findTitleById(CurrentUser.DEMO_USER_ID, resumeId) == null) {
            throw new NoSuchElementException("简历不存在");
        }
        resumeRepository.updateTitle(CurrentUser.DEMO_USER_ID, resumeId, title);
        return buildResumeResponse(CurrentUser.DEMO_USER_ID, resumeId);
    }

    /** 归档：不删除历史与关联；重复归档无副作用。 */
    @Transactional
    public ResumeDTO archiveResume(long resumeId) {
        requireExistingResume(resumeId);
        resumeRepository.updateArchivedAt(CurrentUser.DEMO_USER_ID, resumeId, java.time.LocalDateTime.now());
        return buildResumeResponse(CurrentUser.DEMO_USER_ID, resumeId);
    }

    /** 恢复归档资产；未归档资产恢复无副作用。 */
    @Transactional
    public ResumeDTO restoreResume(long resumeId) {
        requireExistingResume(resumeId);
        resumeRepository.updateArchivedAt(CurrentUser.DEMO_USER_ID, resumeId, null);
        return buildResumeResponse(CurrentUser.DEMO_USER_ID, resumeId);
    }

    private void requireExistingResume(long resumeId) {
        if (resumeRepository.findTitleById(CurrentUser.DEMO_USER_ID, resumeId) == null) {
            throw new NoSuchElementException("简历不存在");
        }
    }

    private String normalizeTitle(String rawTitle) {
        String title = rawTitle == null ? "" : rawTitle.trim();
        if (title.isEmpty()) {
            throw new IllegalArgumentException("简历名称不能为空");
        }
        if (title.length() > MAX_TITLE_LENGTH) {
            throw new IllegalArgumentException("简历名称过长，最大 " + MAX_TITLE_LENGTH + " 字符");
        }
        return title;
    }

    private ResumeDTO buildResumeResponse(long userId, long resumeId) {
        String title = resumeRepository.findTitleById(userId, resumeId);
        String kind = resumeRepository.findKindById(userId, resumeId);
        Long forkedFromVersionId = resumeRepository.findForkedFromVersionIdById(userId, resumeId);
        java.time.LocalDateTime archivedAt = resumeRepository.findArchivedAtById(userId, resumeId);
        Long targetJobDescriptionId = resumeRepository.findTargetJobDescriptionIdById(userId, resumeId);
        Long versionId = resumeRepository.findCurrentVersionId(resumeId);
        ResumeVersionDTO currentVersion = null;
        if (versionId != null) {
            currentVersion = resumeRepository.findVersionById(versionId);
        }
        return new ResumeDTO(resumeId, title, kind, forkedFromVersionId, archivedAt,
                targetJobDescriptionId, currentVersion, null, null);
    }

    public ResumeVersionDTO getVersion(long versionId) {
        ResumeVersionDTO version = resumeRepository.findVersionByIdForUser(CurrentUser.DEMO_USER_ID, versionId);
        if (version == null) {
            throw new IllegalArgumentException("简历版本不存在");
        }
        return version;
    }

    public List<ResumeVersionDTO> getVersions(long resumeId) {
        String title = resumeRepository.findTitleById(CurrentUser.DEMO_USER_ID, resumeId);
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

        String title = resumeRepository.findTitleById(CurrentUser.DEMO_USER_ID, resumeId);
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
