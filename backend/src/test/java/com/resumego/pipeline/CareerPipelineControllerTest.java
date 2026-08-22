package com.resumego.pipeline;

import com.resumego.pipeline.dto.CareerPipelineResponse;
import com.resumego.pipeline.dto.PipelineStageResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CareerPipelineControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean CareerPipelineService service;

    @Test
    void listsCreatesAndTransitionsV2Pipelines() throws Exception {
        when(service.list()).thenReturn(List.of(sample()));
        when(service.create(any())).thenReturn(sample());
        when(service.transition(anyLong(), any())).thenReturn(sample());

        mockMvc.perform(get("/api/v2/pipelines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].companyName").value("腾讯"));
        mockMvc.perform(post("/api/v2/pipelines").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"腾讯 Java","companyName":"腾讯","roleTitle":"Java 后端",
                                 "stages":["准备中","技术面"]}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.lifecycle").value("ACTIVE"));
        mockMvc.perform(post("/api/v2/pipelines/7/transitions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetStageId\":12,\"note\":\"进入技术面\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void archivesAndRestoresPipeline() throws Exception {
        when(service.archive(7L)).thenReturn(sample());
        when(service.restore(7L)).thenReturn(sample());
        mockMvc.perform(post("/api/v2/pipelines/7/archive")).andExpect(status().isOk());
        mockMvc.perform(post("/api/v2/pipelines/7/restore")).andExpect(status().isOk());
    }

    @Test
    void addsRenamesAndReordersPipelineStages() throws Exception {
        when(service.addStage(anyLong(), any())).thenReturn(sample());
        when(service.renameStage(anyLong(), anyLong(), any())).thenReturn(sample());
        when(service.reorderStages(anyLong(), any())).thenReturn(sample());

        mockMvc.perform(post("/api/v2/pipelines/7/stages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"技术二面\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(patch("/api/v2/pipelines/7/stages/13")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"主管面\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/v2/pipelines/7/stages/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"stageIds\":[13,11,12]}"))
                .andExpect(status().isOk());
    }

    @Test
    void linksAndUnlinksScheduleEventsAndInterviewPlans() throws Exception {
        when(service.linkScheduleEvent(7L, 100L)).thenReturn(sample(List.of(100L), List.of()));
        when(service.unlinkScheduleEvent(7L, 100L)).thenReturn(sample());
        when(service.linkInterviewPlan(7L, 300L)).thenReturn(sample(List.of(), List.of(300L)));
        when(service.unlinkInterviewPlan(7L, 300L)).thenReturn(sample());

        mockMvc.perform(put("/api/v2/pipelines/7/schedule-events/100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.scheduleEventIds[0]").value(100));
        mockMvc.perform(delete("/api/v2/pipelines/7/schedule-events/100"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/v2/pipelines/7/interview-plans/300"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.interviewPlanIds[0]").value(300));
        mockMvc.perform(delete("/api/v2/pipelines/7/interview-plans/300"))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsInvalidInputAndMapsDomainErrors() throws Exception {
        mockMvc.perform(post("/api/v2/pipelines").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" \" ,\"companyName\":\"腾讯\",\"roleTitle\":\"Java\"}"))
                .andExpect(status().isBadRequest());
        when(service.get(404L)).thenThrow(new NoSuchElementException("求职管线不存在"));
        mockMvc.perform(get("/api/v2/pipelines/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("求职管线不存在"));
        when(service.transition(anyLong(), any()))
                .thenThrow(new IllegalArgumentException("目标阶段不是待进入状态"));
        mockMvc.perform(post("/api/v2/pipelines/7/transitions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"targetStageId\":12}"))
                .andExpect(status().isBadRequest());
    }

    private CareerPipelineResponse sample() {
        return sample(List.of(), List.of());
    }

    private CareerPipelineResponse sample(List<Long> scheduleEventIds, List<Long> interviewPlanIds) {
        LocalDateTime now = LocalDateTime.now();
        return new CareerPipelineResponse(7L, "腾讯 Java", "腾讯", "Java 后端",
                null, null, PipelineLifecycle.ACTIVE, null, 11L,
                List.of(new PipelineStageResponse(11L, "准备中", 0, PipelineStageState.CURRENT)),
                scheduleEventIds, interviewPlanIds,
                null, now, now);
    }
}
