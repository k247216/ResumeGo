package com.resumego.knowledge;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class KnowledgeCleanupJobControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    KnowledgeRecoveryService recovery;

    @MockBean
    KnowledgeService knowledgeService;

    @MockBean
    KnowledgeClassificationService classification;

    @Test
    void retriesCleanupJob() throws Exception {
        mockMvc.perform(post("/api/v2/knowledge/cleanup-jobs/3/retry"))
                .andExpect(status().isOk());
        verify(recovery).retryCleanupJob(3L);
    }

    @Test
    void missingCleanupJobMapsToNotFound() throws Exception {
        doThrow(new NoSuchElementException("清理任务不存在")).when(recovery).retryCleanupJob(anyLong());
        mockMvc.perform(post("/api/v2/knowledge/cleanup-jobs/404/retry"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("清理任务不存在"));
    }

    @Test
    void wrongStatusMapsToConflict() throws Exception {
        doThrow(new IllegalStateException("NOT_RETRYABLE: 仅 FAILED/PENDING 清理任务可重试"))
                .when(recovery).retryCleanupJob(anyLong());
        mockMvc.perform(post("/api/v2/knowledge/cleanup-jobs/3/retry"))
                .andExpect(status().isConflict());
    }
}
