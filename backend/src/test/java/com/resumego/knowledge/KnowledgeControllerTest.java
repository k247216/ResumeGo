package com.resumego.knowledge;

import com.resumego.knowledge.dto.KnowledgeDocumentResponse;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class KnowledgeControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean KnowledgeService service;

    @Test
    void createsAndListsNoteDocuments() throws Exception {
        when(service.create(any())).thenReturn(sample(7L));
        when(service.list()).thenReturn(List.of(sample(7L)));

        mockMvc.perform(post("/api/v2/knowledge/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"  TensorFlow  笔记 \",\"sourceType\":\"NOTE\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sourceType").value("NOTE"))
                .andExpect(jsonPath("$.data.processingStatus").value("NOT_STARTED"))
                .andExpect(jsonPath("$.data.sourceFile").value((Object) null));

        mockMvc.perform(get("/api/v2/knowledge/documents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("TensorFlow 笔记"));
    }

    @Test
    void getsOwnedDocumentAndMapsMissingToNotFound() throws Exception {
        when(service.get(7L)).thenReturn(sample(7L));
        mockMvc.perform(get("/api/v2/knowledge/documents/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(7));

        when(service.get(404L)).thenThrow(new NoSuchElementException("知识文档不存在"));
        mockMvc.perform(get("/api/v2/knowledge/documents/404"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("知识文档不存在"));
    }

    private KnowledgeDocumentResponse sample(long id) {
        return new KnowledgeDocumentResponse(id, "TensorFlow 笔记", "NOTE", "NOT_STARTED",
                null, LocalDateTime.now().toString(), LocalDateTime.now().toString());
    }
}
