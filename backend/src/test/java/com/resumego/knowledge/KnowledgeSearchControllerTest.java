package com.resumego.knowledge;

import com.resumego.knowledge.dto.KnowledgeSearchItemResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class KnowledgeSearchControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    KnowledgeClassificationService classification;

    @MockBean
    KnowledgeService knowledgeService;

    private KnowledgeSearchItemResponse titleHit() {
        com.resumego.knowledge.dto.KnowledgeDocumentResponse doc =
                new com.resumego.knowledge.dto.KnowledgeDocumentResponse(1L, "TensorFlow 学习笔记", "NOTE", "NOT_STARTED",
                        null, null, LocalDateTime.now().toString(), LocalDateTime.now().toString());
        return new KnowledgeSearchItemResponse(doc, "TITLE", "TensorFlow 学习笔记", null);
    }

    @Test
    void searchesWithQueryAndReturnsMatches() throws Exception {
        when(classification.search(eq("TensorFlow"), isNull(), isNull(), eq(false))).thenReturn(List.of(titleHit()));
        mockMvc.perform(get("/api/v2/knowledge/search").param("q", "TensorFlow"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].matchedField").value("TITLE"))
                .andExpect(jsonPath("$.data[0].document.title").value("TensorFlow 学习笔记"));
    }

    @Test
    void invalidQueryMapsToBadRequest() throws Exception {
        when(classification.search(eq("   "), isNull(), isNull(), eq(false)))
                .thenThrow(new IllegalArgumentException("搜索词长度需为 1-100 个字符"));
        mockMvc.perform(get("/api/v2/knowledge/search").param("q", "   "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void foreignFilterMapsToNotFound() throws Exception {
        when(classification.search(eq("笔记"), eq(99L), isNull(), eq(false)))
                .thenThrow(new NoSuchElementException("分类不存在"));
        mockMvc.perform(get("/api/v2/knowledge/search").param("q", "笔记").param("categoryId", "99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("分类不存在"));
    }

    @Test
    void emptyResultReturnsEmptyArray() throws Exception {
        when(classification.search(eq("无"), isNull(), isNull(), eq(false))).thenReturn(List.of());
        mockMvc.perform(get("/api/v2/knowledge/search").param("q", "无"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void includeDescendantsFlagIsForwarded() throws Exception {
        when(classification.search(eq("笔记"), eq(3L), isNull(), eq(true))).thenReturn(List.of());
        mockMvc.perform(get("/api/v2/knowledge/search").param("q", "笔记")
                        .param("categoryId", "3").param("includeDescendants", "true"))
                .andExpect(status().isOk());
    }
}
