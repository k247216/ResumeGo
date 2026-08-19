package com.resumego.matching.controller;

import com.resumego.matching.dto.MatchDetails;
import com.resumego.matching.dto.MatchRequest;
import com.resumego.matching.dto.MatchResponse;
import com.resumego.matching.service.MatchingPipelineService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 匹配控制器 HTTP 层测试。
 * Mock MatchingPipelineService 以隔离算法逻辑。
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("MatchingController 接口测试")
class MatchingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MatchingPipelineService matchingPipelineService;

    @BeforeEach
    void setUp() {
        // Mock 匹配结果
        MatchResponse mockResponse = MatchResponse.of(0L, 82,
                new MatchDetails()
                        .setRequiredCoverage(80)
                        .setPreferredCoverage(50)
                        .setExperienceCoverage(75)
                        .setEducationMatch(true)
                        .setMatchedItems(List.of("Java", "Spring Boot", "MySQL"))
                        .setMissingItems(List.of("Redis", "Docker"))
                        .setUnknownItems(List.of()));

        when(matchingPipelineService.match(anyLong(), anyLong())).thenReturn(mockResponse);
    }

    @Nested
    @DisplayName("POST 创建匹配")
    class CreateMatch {

        @Test
        @DisplayName("正常请求返回 200 和扁平响应")
        void shouldReturnFlatResponse() throws Exception {
            mockMvc.perform(post("/api/resume-versions/1/job-matches")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"jobDescriptionId\": 1}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.matchScore").value(82))
                    .andExpect(jsonPath("$.details.requiredCoverage").value(80))
                    .andExpect(jsonPath("$.details.preferredCoverage").value(50))
                    .andExpect(jsonPath("$.details.experienceCoverage").value(75))
                    .andExpect(jsonPath("$.details.educationMatch").value(true))
                    .andExpect(jsonPath("$.details.matchedItems[0]").value("Java"))
                    .andExpect(jsonPath("$.details.missingItems[0]").value("Redis"));
        }

        @Test
        @DisplayName("缺少 jobDescriptionId 返回 400")
        void shouldReturn400WhenMissingBody() throws Exception {
            mockMvc.perform(post("/api/resume-versions/1/job-matches")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("jobDescriptionId 为空返回 400")
        void shouldReturn400WhenNullJdId() throws Exception {
            mockMvc.perform(post("/api/resume-versions/1/job-matches")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"jobDescriptionId\": null}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("jobDescriptionId 为负数返回 400")
        void shouldReturn400WhenNegativeJdId() throws Exception {
            mockMvc.perform(post("/api/resume-versions/1/job-matches")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"jobDescriptionId\": -1}"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET 查询匹配")
    class GetMatch {

        @Test
        @DisplayName("当前返回 501 Not Implemented")
        void shouldReturn501() throws Exception {
            mockMvc.perform(get("/api/job-matches/1"))
                    .andExpect(status().isNotImplemented())
                    .andExpect(jsonPath("$.error").value("NOT_IMPLEMENTED"));
        }
    }
}
