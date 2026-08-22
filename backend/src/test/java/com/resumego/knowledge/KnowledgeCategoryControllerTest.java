package com.resumego.knowledge;

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
        return new KnowledgeCategoryResponse(id, "求职", "求职",
                LocalDateTime.now().toString(), LocalDateTime.now().toString());
    }

    @Test
    void listsOwnedCategories() throws Exception {
        when(classification.listCategories()).thenReturn(List.of(sample(1L)));
        mockMvc.perform(get("/api/v2/knowledge/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("求职"));
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
        when(classification.listCategories()).thenThrow(new NoSuchElementException("分类不存在"));
        mockMvc.perform(get("/api/v2/knowledge/categories"))
                .andExpect(status().isNotFound());
    }
}
