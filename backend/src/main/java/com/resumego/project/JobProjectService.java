package com.resumego.project;

import com.resumego.common.CurrentUser;
import com.resumego.project.dto.CreateJobProjectRequest;
import com.resumego.project.dto.JobProjectResponse;
import com.resumego.project.dto.RenameJobProjectRequest;
import com.resumego.project.dto.UpdateJobProjectLinksRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class JobProjectService {

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

    private JobProjectResponse toResponse(JobProject project) {
        return new JobProjectResponse(project.id(), project.name(), project.status(),
                project.jobDescriptionId(), project.resumeVersionId(), project.archivedAt(),
                project.createdAt(), project.updatedAt());
    }
}
