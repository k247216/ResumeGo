package com.resumego.project;

import com.resumego.common.CurrentUser;
import com.resumego.project.dto.CreateJobProjectRequest;
import com.resumego.project.dto.JobProjectResponse;
import com.resumego.project.dto.RenameJobProjectRequest;
import com.resumego.project.dto.StageEventResponse;
import com.resumego.project.dto.UpdateJobProjectApplicationRequest;
import com.resumego.project.dto.UpdateJobProjectLinksRequest;
import com.resumego.project.dto.UpdateJobProjectStageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
public class JobProjectService {

    private static final Set<String> ALLOWED_STAGES =
            Set.of("applied", "exam", "interview", "hr", "offer", "pool", "screened_out", "rejected", "closed");
    /** 流程主线的推进顺序 */
    private static final Map<String, Integer> FLOW_RANK =
            Map.of("applied", 1, "exam", 2, "interview", 3, "hr", 4, "offer", 5);
    /** 终态：进入后锁定，不可再变更 */
    private static final Set<String> TERMINAL_STAGES =
            Set.of("offer", "pool", "screened_out", "rejected", "closed");

    private final JobProjectRepository repository;

    public JobProjectService(JobProjectRepository repository) {
        this.repository = repository;
    }

    public List<JobProjectResponse> list() {
        return repository.findAll(userId()).stream().map(this::toResponse).toList();
    }

    public JobProjectResponse get(long projectId) {
        return toResponse(requireProject(projectId));
    }

    @Transactional
    public JobProjectResponse create(CreateJobProjectRequest request) {
        String name = normalizeName(request.name());
        validateLinks(request.jobDescriptionId(), request.resumeVersionId());
        long id = repository.create(userId(), name, request.jobDescriptionId(), request.resumeVersionId());
        return get(id);
    }

    @Transactional
    public JobProjectResponse rename(long projectId, RenameJobProjectRequest request) {
        requireProject(projectId);
        repository.rename(userId(), projectId, normalizeName(request.name()));
        return get(projectId);
    }

    @Transactional
    public JobProjectResponse updateLinks(long projectId, UpdateJobProjectLinksRequest request) {
        requireProject(projectId);
        validateLinks(request.jobDescriptionId(), request.resumeVersionId());
        repository.updateLinks(userId(), projectId, request.jobDescriptionId(), request.resumeVersionId());
        return get(projectId);
    }

    @Transactional
    public JobProjectResponse updateStage(long projectId, UpdateJobProjectStageRequest request) {
        JobProject project = requireProject(projectId);
        String current = project.stage() == null ? "applied" : project.stage();
        String stage = request.stage() == null ? "" : request.stage().strip();
        if (!ALLOWED_STAGES.contains(stage)) {
            throw new IllegalArgumentException("未知的求职阶段");
        }
        if (stage.equals(current)) {
            return get(projectId);
        }
        if (TERMINAL_STAGES.contains(current)) {
            throw new IllegalArgumentException("该计划已有最终结果，状态已锁定，不可修改");
        }
        Integer currentRank = FLOW_RANK.get(current);
        Integer nextRank = FLOW_RANK.get(stage);
        if (currentRank != null && nextRank != null && nextRank < currentRank) {
            throw new IllegalArgumentException("阶段只能向前推进，不能回退");
        }
        repository.updateStage(userId(), projectId, stage);
        repository.insertStageEvent(userId(), projectId, stage);
        return get(projectId);
    }

    public List<StageEventResponse> listStageEvents(long projectId) {
        requireProject(projectId);
        return repository.findStageEvents(userId(), projectId).stream()
                .map((event) -> new StageEventResponse(event.id(), event.stage(), event.occurredAt()))
                .toList();
    }

    @Transactional
    public JobProjectResponse updateApplicationInfo(long projectId, UpdateJobProjectApplicationRequest request) {
        requireProject(projectId);
        repository.updateApplicationInfo(userId(), projectId,
                normalizeOptional(request.industry()), normalizeOptional(request.role()),
                normalizeOptional(request.location()), normalizeOptional(request.notes()));
        return get(projectId);
    }

    @Transactional
    public JobProjectResponse archive(long projectId) {
        JobProject project = requireProject(projectId);
        if (!"archived".equals(project.status())) {
            repository.archive(userId(), projectId);
        }
        return get(projectId);
    }

    @Transactional
    public JobProjectResponse restore(long projectId) {
        JobProject project = requireProject(projectId);
        if (!"active".equals(project.status())) {
            repository.restore(userId(), projectId);
        }
        return get(projectId);
    }

    @Transactional
    public boolean delete(long projectId) {
        return repository.softDelete(userId(), projectId) > 0;
    }

    private void validateLinks(Long jobDescriptionId, Long resumeVersionId) {
        if (jobDescriptionId != null && !repository.ownsJobDescription(userId(), jobDescriptionId)) {
            throw new IllegalArgumentException("所选目标岗位不可用");
        }
        if (resumeVersionId != null && !repository.ownsResumeVersion(userId(), resumeVersionId)) {
            throw new IllegalArgumentException("所选简历版本不可用");
        }
    }

    private JobProject requireProject(long projectId) {
        return repository.findById(userId(), projectId)
                .orElseThrow(() -> new NoSuchElementException("求职项目不存在"));
    }

    private String normalizeName(String value) {
        String name = value == null ? "" : value.strip();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("项目名称不能为空");
        }
        if (name.length() > 120) {
            throw new IllegalArgumentException("项目名称不能超过 120 个字符");
        }
        return name;
    }

    private long userId() {
        return CurrentUser.DEMO_USER_ID;
    }

    private String normalizeOptional(String value) {
        if (value == null) return null;
        String normalized = value.strip();
        return normalized.isEmpty() ? null : normalized;
    }

    private JobProjectResponse toResponse(JobProject project) {
        return new JobProjectResponse(project.id(), project.name(), project.status(), project.stage(),
                project.jobDescriptionId(), project.resumeVersionId(), project.archivedAt(),
                project.stageUpdatedAt(), project.industry(), project.targetRole(), project.location(),
                project.notes(), project.createdAt(), project.updatedAt());
    }
}
