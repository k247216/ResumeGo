package com.resumego.project;

import com.resumego.project.dto.JobProjectResponse;
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
class JobProjectControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean JobProjectService service;

    private JobProjectResponse sample() {
        return new JobProjectResponse(7L, "Java 实习", "active", 10L, 31L,
                null, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void listsAndCreatesProjects() throws Exception {
        when(service.list()).thenReturn(List.of(sample()));
        when(service.create(any())).thenReturn(sample());

        mockMvc.perform(get("/api/v1/projects"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].id").value(7));
        mockMvc.perform(post("/api/v1/projects").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Java 实习\",\"jobDescriptionId\":10,\"resumeVersionId\":31}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.name").value("Java 实习"));
    }

    @Test
    void rejectsBlankName() throws Exception {
        mockMvc.perform(post("/api/v1/projects").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void exposesRenameLinksArchiveRestoreAndDelete() throws Exception {
        when(service.rename(anyLong(), any())).thenReturn(sample());
        when(service.updateLinks(anyLong(), any())).thenReturn(sample());
        when(service.archive(7L)).thenReturn(sample());
        when(service.restore(7L)).thenReturn(sample());
        when(service.delete(7L)).thenReturn(true);

        mockMvc.perform(patch("/api/v1/projects/7/name").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"新名称\"}")) .andExpect(status().isOk());
        mockMvc.perform(patch("/api/v1/projects/7/links").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"jobDescriptionId\":null,\"resumeVersionId\":null}")) .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/projects/7/archive")).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/projects/7/restore")).andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/projects/7")).andExpect(status().isOk());
    }
}
