package com.resumego.knowledge;

import com.resumego.knowledge.dto.KnowledgeTagResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class KnowledgeTagControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    KnowledgeClassificationService classification;

    @MockBean
    KnowledgeService knowledgeService;

    private KnowledgeTagResponse sample(long id) {
        return new KnowledgeTagResponse(id, "机器学习", "机器学习",
                LocalDateTime.now().toString(), LocalDateTime.now().toString());
    }

    @Test
    void createsTagAndReturns201() throws Exception {
        when(classification.createTag(any()))
                .thenReturn(new KnowledgeNameCreateResult<>(sample(2L), true));
        mockMvc.perform(post("/api/v2/knowledge/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"机器学习\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("机器学习"));
    }

    @Test
    void duplicateTagReturns200WithExisting() throws Exception {
        when(classification.createTag(any()))
                .thenReturn(new KnowledgeNameCreateResult<>(sample(2L), false));
        mockMvc.perform(post("/api/v2/knowledge/tags")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"机器学习\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(2));
    }
}
