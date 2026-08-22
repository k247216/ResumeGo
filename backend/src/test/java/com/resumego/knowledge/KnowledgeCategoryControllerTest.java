package com.resumego.knowledge;

import com.resumego.knowledge.dto.KnowledgeCategoryNodeResponse;
import com.resumego.knowledge.dto.KnowledgeCategoryResponse;
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
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class KnowledgeCategoryControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    KnowledgeClassificationService classification;

    @MockBean
    KnowledgeService knowledgeService;

    private KnowledgeCategoryResponse sample(long id) {
        return new KnowledgeCategoryResponse(id, "求职", "求职", null,
                LocalDateTime.now().toString(), LocalDateTime.now().toString());
    }

    @Test
    void listsOwnedCategoryTreeNodes() throws Exception {
        KnowledgeCategoryNodeResponse node = new KnowledgeCategoryNodeResponse(
                1L, "求职", "求职", null, 0, 2, 5,
                LocalDateTime.now().toString(), LocalDateTime.now().toString());
        when(classification.listCategoryTree()).thenReturn(List.of(node));
        mockMvc.perform(get("/api/v2/knowledge/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("求职"))
                .andExpect(jsonPath("$.data[0].depth").value(0))
                .andExpect(jsonPath("$.data[0].directDocumentCount").value(2))
                .andExpect(jsonPath("$.data[0].descendantDocumentCount").value(5));
    }

    @Test
    void createsCategoryAndReturns201() throws Exception {
        when(classification.createCategory(any()))
                .thenReturn(new KnowledgeNameCreateResult<>(sample(3L), true));
        mockMvc.perform(post("/api/v2/knowledge/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\" 求职 \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(3));
    }

    @Test
    void duplicateNameReturns200WithExisting() throws Exception {
        when(classification.createCategory(any()))
                .thenReturn(new KnowledgeNameCreateResult<>(sample(3L), false));
        mockMvc.perform(post("/api/v2/knowledge/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"求职\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(3));
    }

    @Test
    void blankNameMapsToBadRequest() throws Exception {
        mockMvc.perform(post("/api/v2/knowledge/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("名称不能为空")));
    }

    @Test
    void foreignCategoryMapsToNotFound() throws Exception {
        when(classification.listCategoryTree()).thenThrow(new NoSuchElementException("分类不存在"));
        mockMvc.perform(get("/api/v2/knowledge/categories"))
                .andExpect(status().isNotFound());
    }
}
