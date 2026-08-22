package com.resumego.pipeline;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Import(CareerPipelineRepository.class)
@Sql(scripts = "/sql/career_pipeline_schema.sql")
class CareerPipelineRepositoryTest {

    @Autowired CareerPipelineRepository repository;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void createsIndependentUserScopedPipelinesWithOrderedStages() {
        long first = repository.createPipeline(1L, "腾讯 Java", "腾讯", "Java 后端", 10L, 31L);
        long second = repository.createPipeline(1L, "字节后端", "字节跳动", "后端开发", null, null);
        repository.createStage(first, "准备中", 0, PipelineStageState.CURRENT);
        repository.createStage(first, "技术面", 1, PipelineStageState.PENDING);

        assertThat(repository.findAll(1L)).extracting(CareerPipeline::id)
                .containsExactly(second, first);
        assertThat(repository.findAll(2L)).isEmpty();
        assertThat(repository.findById(2L, first)).isEmpty();
        assertThat(repository.findStages(1L, first)).extracting(PipelineStage::name)
                .containsExactly("准备中", "技术面");
    }

    @Test
    void persistsCurrentStageAndAppendOnlyTransitionHistory() {
        long pipelineId = repository.createPipeline(1L, "腾讯 Java", "腾讯", "Java 后端", null, null);
        long prepare = repository.createStage(pipelineId, "准备中", 0, PipelineStageState.CURRENT);
        long interview = repository.createStage(pipelineId, "技术面", 1, PipelineStageState.PENDING);
        repository.setCurrentStage(1L, pipelineId, prepare);
        repository.appendTransition(pipelineId, null, prepare, "USER", "创建管线");

        repository.updateStageState(pipelineId, prepare, PipelineStageState.COMPLETED);
        repository.updateStageState(pipelineId, interview, PipelineStageState.CURRENT);
        repository.setCurrentStage(1L, pipelineId, interview);
        repository.appendTransition(pipelineId, prepare, interview, "USER", "进入技术面");

        assertThat(repository.findById(1L, pipelineId)).get()
                .extracting(CareerPipeline::currentStageId)
                .isEqualTo(interview);
        assertThat(repository.findTransitions(1L, pipelineId))
                .extracting(PipelineStageTransition::toStageId)
                .containsExactly(prepare, interview);
        assertThat(repository.findStages(1L, pipelineId))
                .extracting(PipelineStage::state)
                .containsExactly(PipelineStageState.COMPLETED, PipelineStageState.CURRENT);
    }

    @Test
    void addsRenamesAndReordersStagesWithoutPositionCollisions() {
        long pipelineId = repository.createPipeline(1L, "腾讯 Java", "腾讯", "Java 后端", null, null);
        long prepare = repository.createStage(pipelineId, "准备中", 0, PipelineStageState.CURRENT);
        long interview = repository.createStage(pipelineId, "技术面", 1, PipelineStageState.PENDING);

        assertThat(repository.nextStagePosition(1L, pipelineId)).isEqualTo(2);
        repository.renameStage(pipelineId, interview, "技术一面");
        repository.reorderStages(pipelineId, java.util.List.of(interview, prepare));

        assertThat(repository.findStages(1L, pipelineId))
                .extracting(PipelineStage::id, PipelineStage::name, PipelineStage::position)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(interview, "技术一面", 0),
                        org.assertj.core.groups.Tuple.tuple(prepare, "准备中", 1));
    }

    @Test
    void validatesLinkedAssetOwnership() {
        assertThat(repository.ownsJobDescription(1L, 10L)).isTrue();
        assertThat(repository.ownsJobDescription(1L, 20L)).isFalse();
        assertThat(repository.ownsResumeVersion(1L, 31L)).isTrue();
        assertThat(repository.ownsResumeVersion(1L, 41L)).isFalse();
    }

    @Test
    void rebindsAndUnlinksScheduleAndInterviewAssets() {
        long first = repository.createPipeline(1L, "腾讯 Java", "腾讯", "Java 后端", null, null);
        long second = repository.createPipeline(1L, "字节后端", "字节", "后端开发", null, null);

        repository.replaceScheduleEventLink(first, 100L);
        repository.replaceInterviewPlanLink(first, 300L);
        repository.replaceScheduleEventLink(second, 100L);
        repository.replaceInterviewPlanLink(second, 300L);

        assertThat(repository.findScheduleEventIds(first)).isEmpty();
        assertThat(repository.findInterviewPlanIds(first)).isEmpty();
        assertThat(repository.findScheduleEventIds(second)).containsExactly(100L);
        assertThat(repository.findInterviewPlanIds(second)).containsExactly(300L);

        assertThat(repository.unlinkScheduleEvent(first, 100L)).isZero();
        assertThat(repository.unlinkInterviewPlan(first, 300L)).isZero();
        assertThat(repository.unlinkScheduleEvent(second, 100L)).isOne();
        assertThat(repository.unlinkInterviewPlan(second, 300L)).isOne();
    }

    @Test
    void omitsSoftDeletedAssetsFromPipelineReads() {
        long pipelineId = repository.createPipeline(1L, "腾讯 Java", "腾讯", "Java 后端", null, null);
        repository.replaceScheduleEventLink(pipelineId, 100L);
        repository.replaceInterviewPlanLink(pipelineId, 300L);
        jdbcTemplate.update("UPDATE schedule_events SET deleted_at = CURRENT_TIMESTAMP WHERE id = 100");
        jdbcTemplate.update("UPDATE interview_plans SET deleted_at = CURRENT_TIMESTAMP WHERE id = 300");

        assertThat(repository.findScheduleEventIds(pipelineId)).isEmpty();
        assertThat(repository.findInterviewPlanIds(pipelineId)).isEmpty();
    }

    @Test
    void findTransitionsReturnsUserScopedHistoryInStableOrder() {
        long first = repository.createPipeline(1L, "腾讯 Java", "腾讯", "Java 后端", null, null);
        long second = repository.createPipeline(2L, "字节后端", "字节跳动", "后端开发", null, null);
        long prepare = repository.createStage(first, "准备中", 0, PipelineStageState.CURRENT);
        long interview = repository.createStage(first, "技术面", 1, PipelineStageState.PENDING);
        repository.appendTransition(first, null, prepare, "USER", "创建管线");
        repository.appendTransition(first, prepare, interview, "USER", "进入技术面");
        // 其他用户的管线不进入当前用户历史
        repository.appendTransition(second, null, prepare, "USER", "另一用户");

        List<PipelineStageTransition> history = repository.findTransitions(1L, first);

        assertThat(history).hasSize(2);
        assertThat(history.get(0).fromStageId()).isNull();
        assertThat(history.get(0).note()).isEqualTo("创建管线");
        assertThat(history.get(1).fromStageId()).isEqualTo(prepare);
        assertThat(history.get(1).toStageId()).isEqualTo(interview);
        // 顺序稳定：occurredAt ASC, id ASC
        assertThat(history.get(0).id()).isLessThan(history.get(1).id());
    }

    @Test
    void findTransitionsReturnsEmptyForPipelineWithoutHistory() {
        long pipelineId = repository.createPipeline(1L, "腾讯 Java", "腾讯", "Java 后端", null, null);
        repository.createStage(pipelineId, "准备中", 0, PipelineStageState.CURRENT);

        assertThat(repository.findTransitions(1L, pipelineId)).isEmpty();
    }
}
