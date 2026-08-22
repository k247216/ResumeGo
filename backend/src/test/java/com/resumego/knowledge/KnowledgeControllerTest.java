package com.resumego.knowledge;

import com.resumego.knowledge.dto.KnowledgeContentResponse;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class KnowledgeControllerTest {

    @Autowired MockMvc mockMvc;
    @MockBean KnowledgeService service;
    @MockBean KnowledgeClassificationService classification;
    @MockBean KnowledgeRecoveryService recovery;
    @MockBean KnowledgeManagedContentService managedContent;

    @Test
    void createsAndListsNoteDocuments() throws Exception {
        when(service.create(any())).thenReturn(sample(7L));
        when(service.list(null, null, false)).thenReturn(List.of(sample(7L)));

        mockMvc.perform(post("/api/v2/knowledge/documents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"  TensorFlow  笔记 \",\"sourceType\":\"NOTE\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.sourceType").value("NOTE"))
                .andExpect(jsonPath("$.data.processingStatus").value("COMPLETED"))
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
        return new KnowledgeDocumentResponse(id, "TensorFlow 笔记", "NOTE", "COMPLETED",
                null, null, LocalDateTime.now().toString(), LocalDateTime.now().toString());
    }

    @Test
    void contentReturnsExtractedTextForOwnedCompletedDocument() throws Exception {
        when(service.getContent(7L)).thenReturn(new KnowledgeContentResponse(7L, "提取的正文内容"));
        mockMvc.perform(get("/api/v2/knowledge/documents/7/content"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.documentId").value(7))
                .andExpect(jsonPath("$.data.content").value("提取的正文内容"));
    }

    @Test
    void contentNotReadyMapsToConflict() throws Exception {
        when(service.getContent(7L)).thenThrow(new IllegalStateException("知识文档尚未完成文本提取"));
        mockMvc.perform(get("/api/v2/knowledge/documents/7/content"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("知识文档尚未完成文本提取"));
    }

    @Test
    void contentMissingOrForeignMapsToNotFound() throws Exception {
        when(service.getContent(404L)).thenThrow(new NoSuchElementException("知识文档不存在"));
        mockMvc.perform(get("/api/v2/knowledge/documents/404/content"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("知识文档不存在"));
    }

    @Test
    void managesDocumentCategoryAndTagsRelationships() throws Exception {
        mockMvc.perform(put("/api/v2/knowledge/documents/7/category/3"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/v2/knowledge/documents/7/category/3"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/v2/knowledge/documents/7/tags/5"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/v2/knowledge/documents/7/tags/5"))
                .andExpect(status().isOk());
    }

    @Test
    void foreignRelationMapsToNotFound() throws Exception {
        org.mockito.Mockito.doThrow(new NoSuchElementException("知识文档不存在"))
                .when(classification).setDocumentCategory(404L, 1L);
        mockMvc.perform(put("/api/v2/knowledge/documents/404/category/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("知识文档不存在"));

        org.mockito.Mockito.doThrow(new NoSuchElementException("标签不存在"))
                .when(classification).addDocumentTag(7L, 99L);
        mockMvc.perform(put("/api/v2/knowledge/documents/7/tags/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void readsDocumentClassificationHonestly() throws Exception {
        com.resumego.knowledge.dto.KnowledgeDocumentClassificationResponse resp =
                new com.resumego.knowledge.dto.KnowledgeDocumentClassificationResponse(null, java.util.List.of());
        org.mockito.Mockito.when(classification.getDocumentClassification(7L)).thenReturn(resp);
        mockMvc.perform(get("/api/v2/knowledge/documents/7/classification"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.category").value((Object) null))
                .andExpect(jsonPath("$.data.tags").isArray())
                .andExpect(jsonPath("$.data.tags.length()").value(0));
    }

    @Test
    void foreignClassificationMapsToNotFound() throws Exception {
        org.mockito.Mockito.when(classification.getDocumentClassification(404L))
                .thenThrow(new NoSuchElementException("知识文档不存在"));
        mockMvc.perform(get("/api/v2/knowledge/documents/404/classification"))
                .andExpect(status().isNotFound());
    }

    @Test
    void retriesFailedFileDocument() throws Exception {
        org.mockito.Mockito.when(recovery.retry(7L)).thenReturn(sample(7L));
        mockMvc.perform(post("/api/v2/knowledge/documents/7/retry"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(7));
    }

    @Test
    void nonRetryableStateMapsToConflict() throws Exception {
        org.mockito.Mockito.when(recovery.retry(7L))
                .thenThrow(new IllegalStateException("NOT_RETRYABLE: 仅 FAILED 状态的文档可重试"));
        mockMvc.perform(post("/api/v2/knowledge/documents/7/retry"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("NOT_RETRYABLE")));
    }

    @Test
    void deletionImpactReturnsSummaryWithoutPaths() throws Exception {
        org.mockito.Mockito.when(recovery.deletionImpact(7L)).thenReturn(
                new com.resumego.knowledge.dto.KnowledgeDeletionImpactResponse(
                        "标题", true, true, false, true, "abc123", java.time.LocalDateTime.now()));
        mockMvc.perform(get("/api/v2/knowledge/documents/7/deletion-impact"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("标题"))
                .andExpect(jsonPath("$.data.hasSource").value(true))
                .andExpect(jsonPath("$.data.confirmationToken").value("abc123"));
    }

    @Test
    void deletesDocumentWithConfirmationToken() throws Exception {
        mockMvc.perform(delete("/api/v2/knowledge/documents/7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmationToken\":\"abc123\"}"))
                .andExpect(status().isOk());
        org.mockito.Mockito.verify(recovery).deleteDocument(7L, "abc123");
    }

    @Test
    void savesNoteContentViaPut() throws Exception {
        org.mockito.Mockito.when(managedContent.saveContent(7L, "笔记正文"))
                .thenReturn(new KnowledgeContentResponse(7L, "笔记正文"));
        mockMvc.perform(put("/api/v2/knowledge/documents/7/content")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"笔记正文\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("笔记正文"));
    }

    @Test
    void noteContentFileRejectionMapsToConflict() throws Exception {
        org.mockito.Mockito.when(managedContent.saveContent(6L, "x"))
                .thenThrow(new IllegalStateException("NOT_EDITABLE: 仅 Markdown 文件可编辑，TXT 只读"));
        mockMvc.perform(put("/api/v2/knowledge/documents/6/content")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"x\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void noteContentTooLargeMapsToBadRequest() throws Exception {
        org.mockito.Mockito.when(managedContent.saveContent(7L, "a".repeat(1024 * 1024 + 1)))
                .thenThrow(new IllegalArgumentException("正文不能超过 1 MiB"));
        mockMvc.perform(put("/api/v2/knowledge/documents/7/content")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"" + "a".repeat(1024 * 1024 + 1) + "\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listsDocumentsWithBrowseFilters() throws Exception {
        when(service.list(3L, null, true)).thenReturn(List.of(sample(7L)));
        mockMvc.perform(get("/api/v2/knowledge/documents")
                        .param("categoryId", "3").param("includeDescendants", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(7));

        when(service.list(null, 5L, false)).thenReturn(List.of(sample(8L)));
        mockMvc.perform(get("/api/v2/knowledge/documents").param("tagId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(8));
    }
}
