package com.resumego.pipeline;

import com.resumego.pipeline.dto.CreateCareerPipelineRequest;
import com.resumego.pipeline.dto.AddPipelineStageRequest;
import com.resumego.pipeline.dto.RenamePipelineStageRequest;
import com.resumego.pipeline.dto.ReorderPipelineStagesRequest;
import com.resumego.pipeline.dto.TransitionPipelineStageRequest;
import com.resumego.pipeline.port.PipelineInterviewPlanAccess;
import com.resumego.pipeline.port.PipelineScheduleEventAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class CareerPipelineServiceTest {

    private CareerPipelineRepository repository;
    private CareerPipelineService service;

    @BeforeEach
    void setUp() {
        repository = mock(CareerPipelineRepository.class);
        service = new CareerPipelineService(repository, new PipelineRules(),
                mock(PipelineScheduleEventAccess.class), mock(PipelineInterviewPlanAccess.class));
    }

    @Test
    void createsNormalizedPipelineWithCustomStages() {
        when(repository.ownsJobDescription(1L, 10L)).thenReturn(true);
        when(repository.ownsResumeVersion(1L, 31L)).thenReturn(true);
        when(repository.createPipeline(1L, "腾讯 Java", "腾讯", "Java 后端", 10L, 31L)).thenReturn(7L);
        when(repository.createStage(7L, "准备中", 0, PipelineStageState.CURRENT)).thenReturn(11L);
        when(repository.createStage(7L, "技术面", 1, PipelineStageState.PENDING)).thenReturn(12L);
        stubPipeline(7L, 11L, PipelineLifecycle.ACTIVE);

        service.create(new CreateCareerPipelineRequest(
                " 腾讯 Java ", " 腾讯 ", " Java 后端 ", 10L, 31L,
                List.of(" 准备中 ", " 技术面 ")));

        verify(repository).setCurrentStage(1L, 7L, 11L);
        verify(repository).appendTransition(7L, null, 11L, "USER", "创建求职管线");
    }

    @Test
    void rejectsBlankOrDuplicateStagesAndForeignAssets() {
        assertThatThrownBy(() -> service.create(new CreateCareerPipelineRequest(
                "目标", "公司", "岗位", null, null, List.of("准备", " 准备 "))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("重复");
        assertThatThrownBy(() -> service.create(new CreateCareerPipelineRequest(
                "目标", "公司", "岗位", null, null, List.of("准备", "  "))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("阶段");

        when(repository.ownsJobDescription(1L, 20L)).thenReturn(false);
        assertThatThrownBy(() -> service.create(new CreateCareerPipelineRequest(
                "目标", "公司", "岗位", 20L, null, List.of("准备"))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("岗位");
    }

    @Test
    void transitionsOnlyToOwnedPendingStageAndAppendsHistory() {
        stubPipeline(7L, 11L, PipelineLifecycle.ACTIVE);
        when(repository.findStage(1L, 7L, 12L)).thenReturn(Optional.of(stage(12L, PipelineStageState.PENDING)));

        service.transition(7L, new TransitionPipelineStageRequest(12L, "进入技术面"));

        verify(repository).updateStageState(7L, 11L, PipelineStageState.COMPLETED);
        verify(repository).updateStageState(7L, 12L, PipelineStageState.CURRENT);
        verify(repository).setCurrentStage(1L, 7L, 12L);
        verify(repository).appendTransition(7L, 11L, 12L, "USER", "进入技术面");
    }

    @Test
    void blocksTransitionWhenArchivedOrTargetIsNotPending() {
        stubPipeline(7L, 11L, PipelineLifecycle.ARCHIVED);
        when(repository.findStage(1L, 7L, 12L)).thenReturn(Optional.of(stage(12L, PipelineStageState.PENDING)));
        assertThatThrownBy(() -> service.transition(7L, new TransitionPipelineStageRequest(12L, null)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("归档");

        stubPipeline(8L, 21L, PipelineLifecycle.ACTIVE);
        when(repository.findStage(1L, 8L, 22L)).thenReturn(Optional.of(
                new PipelineStage(22L, 8L, "旧阶段", 0, PipelineStageState.COMPLETED,
                        LocalDateTime.now(), LocalDateTime.now())));
        assertThatThrownBy(() -> service.transition(8L, new TransitionPipelineStageRequest(22L, null)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("待进入");
    }

    @Test
    void addsRenamesAndReordersStagesWithinOwnedPipeline() {
        stubPipeline(7L, 11L, PipelineLifecycle.ACTIVE);
        when(repository.nextStagePosition(1L, 7L)).thenReturn(2);
        when(repository.createStage(7L, "二面", 2, PipelineStageState.PENDING)).thenReturn(13L);
        when(repository.findStage(1L, 7L, 13L)).thenReturn(Optional.of(stage(13L, PipelineStageState.PENDING)));
        List<PipelineStage> before = List.of(
                new PipelineStage(11L, 7L, "准备中", 0, PipelineStageState.CURRENT,
                        LocalDateTime.now(), LocalDateTime.now()),
                new PipelineStage(12L, 7L, "技术面", 1, PipelineStageState.PENDING,
                        LocalDateTime.now(), LocalDateTime.now()));
        List<PipelineStage> after = List.of(
                before.get(0), before.get(1),
                new PipelineStage(13L, 7L, "二面", 2, PipelineStageState.PENDING,
                        LocalDateTime.now(), LocalDateTime.now()));
        when(repository.findStages(1L, 7L)).thenReturn(before, after, after, after, after, after);

        service.addStage(7L, new AddPipelineStageRequest(" 二面 "));
        service.renameStage(7L, 13L, new RenamePipelineStageRequest(" 技术二面 "));
        service.reorderStages(7L, new ReorderPipelineStagesRequest(List.of(13L, 11L, 12L)));

        verify(repository).createStage(7L, "二面", 2, PipelineStageState.PENDING);
        verify(repository).renameStage(7L, 13L, "技术二面");
        verify(repository).reorderStages(7L, List.of(13L, 11L, 12L));
    }

    @Test
    void rejectsIncompleteOrForeignStageOrder() {
        stubPipeline(7L, 11L, PipelineLifecycle.ACTIVE);
        when(repository.findStages(1L, 7L)).thenReturn(List.of(
                new PipelineStage(11L, 7L, "准备中", 0, PipelineStageState.CURRENT,
                        LocalDateTime.now(), LocalDateTime.now()),
                new PipelineStage(12L, 7L, "技术面", 1, PipelineStageState.PENDING,
                        LocalDateTime.now(), LocalDateTime.now())));

        assertThatThrownBy(() -> service.reorderStages(
                7L, new ReorderPipelineStagesRequest(List.of(11L))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("全部阶段");
        assertThatThrownBy(() -> service.reorderStages(
                7L, new ReorderPipelineStagesRequest(List.of(11L, 99L))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("全部阶段");
    }

    private void stubPipeline(long id, long currentStageId, PipelineLifecycle lifecycle) {
        var pipeline = new CareerPipeline(id, 1L, "腾讯 Java", "腾讯", "Java 后端",
                null, null, lifecycle, null, currentStageId, null,
                LocalDateTime.now(), LocalDateTime.now());
        when(repository.findById(1L, id)).thenReturn(Optional.of(pipeline));
        when(repository.findStages(1L, id)).thenReturn(List.of(
                new PipelineStage(currentStageId, id, "准备中", 0, PipelineStageState.CURRENT,
                        LocalDateTime.now(), LocalDateTime.now())));
    }

    private PipelineStage stage(long id, PipelineStageState state) {
        return new PipelineStage(id, 7L, "技术面", 1, state,
                LocalDateTime.now(), LocalDateTime.now());
    }
}
