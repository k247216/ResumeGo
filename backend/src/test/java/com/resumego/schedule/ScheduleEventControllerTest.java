package com.resumego.schedule;

import com.resumego.schedule.dto.ScheduleEventResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ScheduleEventControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean ScheduleEventService service;

    private ScheduleEventResponse sample() {
        return new ScheduleEventResponse(7L, "腾讯技术面", "interview",
                LocalDateTime.of(2026, 8, 25, 14, 0), null, "第一轮", 10L, 100L,
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void listsAndCreatesEvents() throws Exception {
        when(service.list(any(), any())).thenReturn(List.of(sample()));
        when(service.create(any())).thenReturn(sample());

        mockMvc.perform(get("/api/v1/schedule-events").param("from", "2026-08-01T00:00:00")
                        .param("to", "2026-09-01T00:00:00"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value(7));
        mockMvc.perform(post("/api/v1/schedule-events").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"腾讯技术面\",\"eventType\":\"interview\",\"startTime\":\"2026-08-25T14:00:00\",\"notes\":\"第一轮\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.title").value("腾讯技术面"));
    }

    @Test
    void rejectsBlankTitle() throws Exception {
        mockMvc.perform(post("/api/v1/schedule-events").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"   \",\"eventType\":\"interview\",\"startTime\":\"2026-08-25T14:00:00\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void exposesUpdateGetAndDelete() throws Exception {
        when(service.update(anyLong(), any())).thenReturn(sample());
        when(service.get(7L)).thenReturn(sample());
        when(service.delete(7L)).thenReturn(true);

        mockMvc.perform(patch("/api/v1/schedule-events/7").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"腾讯技术面\",\"eventType\":\"interview\",\"startTime\":\"2026-08-25T14:00:00\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/schedule-events/7")).andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/schedule-events/7")).andExpect(status().isOk());
    }
}
