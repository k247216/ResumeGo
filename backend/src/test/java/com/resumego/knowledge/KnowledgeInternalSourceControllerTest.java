package com.resumego.knowledge;

import com.resumego.knowledge.dto.KnowledgeManagedSourceResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "resumego.security.internal-token=test-internal-token")
class KnowledgeInternalSourceControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    KnowledgeInternalSourceService service;

    @Test
    void missingOrWrongInternalTokenIsForbidden() throws Exception {
        mockMvc.perform(get("/api/v2/internal/knowledge/documents/7/managed-source"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v2/internal/knowledge/documents/7/managed-source")
                        .header("X-ResumeGo-Internal", "wrong"))
                .andExpect(status().isForbidden());
    }

    @Test
    void validTokenReturnsManagedRelativePath() throws Exception {
        when(service.managedSource(7L)).thenReturn(new KnowledgeManagedSourceResponse("knowledge/sources/1/x.md"));
        mockMvc.perform(get("/api/v2/internal/knowledge/documents/7/managed-source")
                        .header("X-ResumeGo-Internal", "test-internal-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.relativePath").value("knowledge/sources/1/x.md"));
    }

    @Test
    void stableFailureCodesMapToNotFound() throws Exception {
        when(service.managedSource(anyLong())).thenThrow(new ManagedSourceException("SOURCE_MISSING"));
        mockMvc.perform(get("/api/v2/internal/knowledge/documents/7/managed-source")
                        .header("X-ResumeGo-Internal", "test-internal-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("SOURCE_MISSING"));
    }
}
