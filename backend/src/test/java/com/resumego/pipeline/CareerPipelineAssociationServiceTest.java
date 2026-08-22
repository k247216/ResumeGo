package com.resumego.pipeline;

import com.resumego.pipeline.port.PipelineInterviewPlanAccess;
import com.resumego.pipeline.port.PipelineScheduleEventAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JdbcTest
@Import(CareerPipelineRepository.class)
@Sql(scripts = "/sql/career_pipeline_schema.sql")
class CareerPipelineAssociationServiceTest {

    @Autowired CareerPipelineRepository repository;
    CareerPipelineService service;

    @BeforeEach
    void setUp() {
        PipelineScheduleEventAccess schedules = (userId, eventId) -> userId == 1L && eventId == 100L;
        PipelineInterviewPlanAccess interviews = (userId, planId) -> userId == 1L && planId == 300L;
        service = new CareerPipelineService(repository, new PipelineRules(), schedules, interviews);
    }

    @Test
    void linksOwnedAssetsAndMovesEachAssetBetweenPipelines() {
        long first = createPipeline("腾讯 Java");
        long second = createPipeline("字节后端");

        service.linkScheduleEvent(first, 100L);
        service.linkInterviewPlan(first, 300L);
        var response = service.linkScheduleEvent(second, 100L);
        response = service.linkInterviewPlan(second, 300L);

        assertThat(service.get(first).scheduleEventIds()).isEmpty();
        assertThat(service.get(first).interviewPlanIds()).isEmpty();
        assertThat(response.scheduleEventIds()).containsExactly(100L);
        assertThat(response.interviewPlanIds()).containsExactly(300L);
    }

    @Test
    void rejectsAssetsOutsideTheCurrentLocalUser() {
        long pipelineId = createPipeline("腾讯 Java");

        assertThatThrownBy(() -> service.linkScheduleEvent(pipelineId, 200L))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("日程");
        assertThatThrownBy(() -> service.linkInterviewPlan(pipelineId, 400L))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("面试");
        assertThat(service.get(pipelineId).scheduleEventIds()).isEmpty();
        assertThat(service.get(pipelineId).interviewPlanIds()).isEmpty();
    }

    @Test
    void unlinksOnlyFromTheOwnedPipeline() {
        long pipelineId = createPipeline("腾讯 Java");
        service.linkScheduleEvent(pipelineId, 100L);
        service.linkInterviewPlan(pipelineId, 300L);

        var response = service.unlinkScheduleEvent(pipelineId, 100L);
        response = service.unlinkInterviewPlan(pipelineId, 300L);

        assertThat(response.scheduleEventIds()).isEmpty();
        assertThat(response.interviewPlanIds()).isEmpty();
    }

    private long createPipeline(String name) {
        long pipelineId = repository.createPipeline(1L, name, "公司", "后端开发", null, null);
        long stageId = repository.createStage(pipelineId, "准备中", 0, PipelineStageState.CURRENT);
        repository.setCurrentStage(1L, pipelineId, stageId);
        return pipelineId;
    }
}
