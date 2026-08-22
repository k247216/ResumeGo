package com.resumego.pipeline;

import com.resumego.common.CurrentUser;
import com.resumego.pipeline.dto.CareerPipelineResponse;
import com.resumego.pipeline.dto.CreateCareerPipelineRequest;
import com.resumego.pipeline.dto.AddPipelineStageRequest;
import com.resumego.pipeline.dto.PipelineStageResponse;
import com.resumego.pipeline.dto.PipelineStageTransitionResponse;
import com.resumego.pipeline.dto.RenamePipelineStageRequest;
import com.resumego.pipeline.dto.ReorderPipelineStagesRequest;
import com.resumego.pipeline.dto.TransitionPipelineStageRequest;
import com.resumego.pipeline.port.PipelineInterviewPlanAccess;
import com.resumego.pipeline.port.PipelineScheduleEventAccess;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.NoSuchElementException;

@Service
public class CareerPipelineService {

    private static final List<String> DEFAULT_STAGES = List.of(
            "准备中", "已投递", "笔试", "技术面", "HR 面", "Offer");

    private final CareerPipelineRepository repository;
    private final PipelineRules rules;
    private final PipelineScheduleEventAccess scheduleEventAccess;
    private final PipelineInterviewPlanAccess interviewPlanAccess;

    public CareerPipelineService(CareerPipelineRepository repository, PipelineRules rules,
                                 PipelineScheduleEventAccess scheduleEventAccess,
                                 PipelineInterviewPlanAccess interviewPlanAccess) {
        this.repository = repository;
        this.rules = rules;
        this.scheduleEventAccess = scheduleEventAccess;
        this.interviewPlanAccess = interviewPlanAccess;
    }

    public List<CareerPipelineResponse> list() {
        return repository.findAll(userId()).stream().map(this::toResponse).toList();
    }

    public CareerPipelineResponse get(long pipelineId) {
        return toResponse(requirePipeline(pipelineId));
    }

    public List<PipelineStageTransitionResponse> findTransitionHistory(long pipelineId) {
        requirePipeline(pipelineId);
        return repository.findTransitions(userId(), pipelineId).stream()
                .map(transition -> new PipelineStageTransitionResponse(
                        transition.id(),
                        transition.pipelineId(),
                        transition.fromStageId(),
                        transition.toStageId(),
                        transition.actor(),
                        transition.note(),
                        transition.occurredAt()))
                .toList();
    }

    @Transactional
    public CareerPipelineResponse create(CreateCareerPipelineRequest request) {
        String name = normalize(request.name(), "管线名称", 120);
        String company = normalize(request.companyName(), "公司", 120);
        String role = normalize(request.roleTitle(), "岗位", 160);
        validateLinks(request.jobDescriptionId(), request.resumeVersionId());
        List<String> stages = normalizeStages(request.stages());

        long pipelineId = repository.createPipeline(userId(), name, company, role,
                request.jobDescriptionId(), request.resumeVersionId());
        long firstStageId = 0;
        for (int index = 0; index < stages.size(); index++) {
            long stageId = repository.createStage(pipelineId, stages.get(index), index,
                    index == 0 ? PipelineStageState.CURRENT : PipelineStageState.PENDING);
            if (index == 0) firstStageId = stageId;
        }
        repository.setCurrentStage(userId(), pipelineId, firstStageId);
        repository.appendTransition(pipelineId, null, firstStageId, "USER", "创建求职管线");
        return get(pipelineId);
    }

    @Transactional
    public CareerPipelineResponse transition(long pipelineId, TransitionPipelineStageRequest request) {
        CareerPipeline pipeline = requirePipeline(pipelineId);
        if (pipeline.currentStageId() == null) {
            throw new IllegalStateException("求职管线没有当前阶段");
        }
        PipelineStage target = repository.findStage(userId(), pipelineId, request.targetStageId())
                .orElseThrow(() -> new NoSuchElementException("目标阶段不存在"));
        rules.validateStageTransition(pipeline.currentStageId(), target.id(), pipeline.lifecycle());
        if (target.state() != PipelineStageState.PENDING) {
            throw new IllegalArgumentException("目标阶段不是待进入状态");
        }
        String note = normalizeOptional(request.note(), 500);
        repository.updateStageState(pipelineId, pipeline.currentStageId(), PipelineStageState.COMPLETED);
        repository.updateStageState(pipelineId, target.id(), PipelineStageState.CURRENT);
        repository.setCurrentStage(userId(), pipelineId, target.id());
        repository.appendTransition(pipelineId, pipeline.currentStageId(), target.id(), "USER", note);
        return get(pipelineId);
    }

    @Transactional
    public CareerPipelineResponse addStage(long pipelineId, AddPipelineStageRequest request) {
        CareerPipeline pipeline = requireEditablePipeline(pipelineId);
        String name = normalize(request.name(), "阶段名称", 80);
        rejectDuplicateStageName(repository.findStages(userId(), pipeline.id()), name, null);
        int position = repository.nextStagePosition(userId(), pipeline.id());
        repository.createStage(pipeline.id(), name, position, PipelineStageState.PENDING);
        return get(pipeline.id());
    }

    @Transactional
    public CareerPipelineResponse renameStage(long pipelineId, long stageId,
                                               RenamePipelineStageRequest request) {
        CareerPipeline pipeline = requireEditablePipeline(pipelineId);
        repository.findStage(userId(), pipeline.id(), stageId)
                .orElseThrow(() -> new NoSuchElementException("管线阶段不存在"));
        String name = normalize(request.name(), "阶段名称", 80);
        rejectDuplicateStageName(repository.findStages(userId(), pipeline.id()), name, stageId);
        repository.renameStage(pipeline.id(), stageId, name);
        return get(pipeline.id());
    }

    @Transactional
    public CareerPipelineResponse reorderStages(long pipelineId, ReorderPipelineStagesRequest request) {
        CareerPipeline pipeline = requireEditablePipeline(pipelineId);
        List<PipelineStage> stages = repository.findStages(userId(), pipeline.id());
        List<Long> requestedIds = request.stageIds();
        if (requestedIds == null
                || requestedIds.size() != stages.size()
                || new HashSet<>(requestedIds).size() != requestedIds.size()
                || !new HashSet<>(requestedIds).equals(
                        stages.stream().map(PipelineStage::id).collect(java.util.stream.Collectors.toSet()))) {
            throw new IllegalArgumentException("排序必须包含管线的全部阶段且不能重复");
        }
        repository.reorderStages(pipeline.id(), requestedIds);
        return get(pipeline.id());
    }

    @Transactional
    public CareerPipelineResponse linkScheduleEvent(long pipelineId, long eventId) {
        CareerPipeline pipeline = requireEditablePipeline(pipelineId);
        if (!scheduleEventAccess.existsForUser(userId(), eventId)) {
            throw new IllegalArgumentException("所选日程不可用");
        }
        repository.replaceScheduleEventLink(pipeline.id(), eventId);
        return get(pipeline.id());
    }

    @Transactional
    public CareerPipelineResponse unlinkScheduleEvent(long pipelineId, long eventId) {
        CareerPipeline pipeline = requireEditablePipeline(pipelineId);
        repository.unlinkScheduleEvent(pipeline.id(), eventId);
        return get(pipeline.id());
    }

    @Transactional
    public CareerPipelineResponse linkInterviewPlan(long pipelineId, long planId) {
        CareerPipeline pipeline = requireEditablePipeline(pipelineId);
        if (!interviewPlanAccess.existsForUser(userId(), planId)) {
            throw new IllegalArgumentException("所选模拟面试不可用");
        }
        repository.replaceInterviewPlanLink(pipeline.id(), planId);
        return get(pipeline.id());
    }

    @Transactional
    public CareerPipelineResponse unlinkInterviewPlan(long pipelineId, long planId) {
        CareerPipeline pipeline = requireEditablePipeline(pipelineId);
        repository.unlinkInterviewPlan(pipeline.id(), planId);
        return get(pipeline.id());
    }

    @Transactional
    public CareerPipelineResponse archive(long pipelineId) {
        requirePipeline(pipelineId);
        repository.updateLifecycle(userId(), pipelineId, PipelineLifecycle.ARCHIVED, null);
        return get(pipelineId);
    }

    @Transactional
    public CareerPipelineResponse restore(long pipelineId) {
        requirePipeline(pipelineId);
        repository.updateLifecycle(userId(), pipelineId, PipelineLifecycle.ACTIVE, null);
        return get(pipelineId);
    }

    private CareerPipeline requirePipeline(long pipelineId) {
        return repository.findById(userId(), pipelineId)
                .orElseThrow(() -> new NoSuchElementException("求职管线不存在"));
    }

    private CareerPipeline requireEditablePipeline(long pipelineId) {
        CareerPipeline pipeline = requirePipeline(pipelineId);
        if (pipeline.lifecycle() == PipelineLifecycle.ARCHIVED) {
            throw new IllegalStateException("已归档的求职管线不能修改阶段");
        }
        if (pipeline.lifecycle() == PipelineLifecycle.CLOSED) {
            throw new IllegalStateException("已结束的求职管线不能修改阶段");
        }
        return pipeline;
    }

    private void rejectDuplicateStageName(List<PipelineStage> stages, String name, Long ignoredStageId) {
        boolean duplicate = stages.stream().anyMatch(stage -> stage.name().equals(name)
                && (ignoredStageId == null || stage.id() != ignoredStageId));
        if (duplicate) throw new IllegalArgumentException("阶段名称不能重复");
    }

    private void validateLinks(Long jobDescriptionId, Long resumeVersionId) {
        if (jobDescriptionId != null && !repository.ownsJobDescription(userId(), jobDescriptionId)) {
            throw new IllegalArgumentException("所选目标岗位不可用");
        }
        if (resumeVersionId != null && !repository.ownsResumeVersion(userId(), resumeVersionId)) {
            throw new IllegalArgumentException("所选简历版本不可用");
        }
    }

    private List<String> normalizeStages(List<String> values) {
        List<String> source = values == null || values.isEmpty() ? DEFAULT_STAGES : values;
        List<String> normalized = source.stream()
                .map(value -> normalize(value, "阶段名称", 80))
                .toList();
        if (new HashSet<>(normalized).size() != normalized.size()) {
            throw new IllegalArgumentException("阶段名称不能重复");
        }
        return normalized;
    }

    private String normalize(String value, String label, int maxLength) {
        String normalized = value == null ? "" : value.strip();
        if (normalized.isEmpty()) throw new IllegalArgumentException(label + "不能为空");
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(label + "不能超过 " + maxLength + " 个字符");
        }
        return normalized;
    }

    private String normalizeOptional(String value, int maxLength) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.strip();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException("阶段说明不能超过 " + maxLength + " 个字符");
        }
        return normalized;
    }

    private CareerPipelineResponse toResponse(CareerPipeline pipeline) {
        List<PipelineStageResponse> stages = repository.findStages(userId(), pipeline.id()).stream()
                .map(stage -> new PipelineStageResponse(stage.id(), stage.name(), stage.position(), stage.state()))
                .toList();
        return new CareerPipelineResponse(
                pipeline.id(), pipeline.name(), pipeline.companyName(), pipeline.roleTitle(),
                pipeline.jobDescriptionId(), pipeline.resumeVersionId(), pipeline.lifecycle(),
                pipeline.outcome(), pipeline.currentStageId(), stages,
                repository.findScheduleEventIds(pipeline.id()),
                repository.findInterviewPlanIds(pipeline.id()),
                pipeline.archivedAt(), pipeline.createdAt(), pipeline.updatedAt());
    }

    private long userId() {
        return CurrentUser.DEMO_USER_ID;
    }
}
