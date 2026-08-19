package com.resumego.job;

import com.resumego.job.dto.JobDescriptionDTO;
import com.resumego.job.dto.CreateJobDescriptionRequest;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("JobDescriptionController 接口测试")
class JobDescriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JobDescriptionService jobDescriptionService;

    private JobDescriptionDTO sampleJd;

    @BeforeEach
    void setUp() {
        sampleJd = new JobDescriptionDTO();
        sampleJd.setId(1L);
        sampleJd.setJobTitle("Java 开发工程师");
        sampleJd.setCompanyName("某科技公司");
        sampleJd.setRawText("负责后端系统设计与开发，要求熟练掌握 Java 和 Spring Boot");
        sampleJd.setParseStatus("succeeded");
        sampleJd.setCreatedAt(LocalDateTime.now());
        sampleJd.setUpdatedAt(LocalDateTime.now());
    }

    @Nested
    @DisplayName("GET 列表")
    class ListJds {

        @Test
        @DisplayName("正常返回 JD 列表")
        void shouldReturnList() throws Exception {
            when(jobDescriptionService.findAllByUser()).thenReturn(List.of(sampleJd));

            mockMvc.perform(get("/api/v1/job-descriptions"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data[0].id").value(1))
                    .andExpect(jsonPath("$.data[0].jobTitle").value("Java 开发工程师"));
        }
    }

    @Nested
    @DisplayName("GET 详情")
    class GetById {

        @Test
        @DisplayName("存在的 JD 返回 200")
        void shouldReturnJdWhenFound() throws Exception {
            when(jobDescriptionService.findById(1L)).thenReturn(sampleJd);

            mockMvc.perform(get("/api/v1/job-descriptions/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.jobTitle").value("Java 开发工程师"));
        }

        @Test
        @DisplayName("不存在的 JD 返回 404")
        void shouldReturn404WhenNotFound() throws Exception {
            when(jobDescriptionService.findById(999L)).thenReturn(null);

            mockMvc.perform(get("/api/v1/job-descriptions/999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST 创建")
    class CreateJd {

        @Test
        @DisplayName("正常创建返回 201")
        void shouldCreateJd() throws Exception {
            when(jobDescriptionService.create(any(CreateJobDescriptionRequest.class))).thenReturn(sampleJd);

            mockMvc.perform(post("/api/v1/job-descriptions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "jobTitle": "Java 开发工程师",
                                      "companyName": "某科技公司",
                                      "rawText": "负责后端系统设计与开发，要求熟练掌握 Java 和 Spring Boot"
                                    }"""))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.data.id").value(1));
        }

        @Test
        @DisplayName("缺少必填字段返回 400")
        void shouldReturn400WhenMissingRequired() throws Exception {
            mockMvc.perform(post("/api/v1/job-descriptions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("rawText 过短返回 400")
        void shouldReturn400WhenRawTextTooShort() throws Exception {
            mockMvc.perform(post("/api/v1/job-descriptions")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "jobTitle": "Java",
                                      "rawText": "短"
                                    }"""))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST 解析")
    class ParseJd {

        @Test
        @DisplayName("正常解析返回 200")
        void shouldParseJd() throws Exception {
            when(jobDescriptionService.parse(1L)).thenReturn(sampleJd);

            mockMvc.perform(post("/api/v1/job-descriptions/1/parse"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.parseStatus").value("succeeded"));
        }

        @Test
        @DisplayName("JD 不存在返回 404")
        void shouldReturn404WhenJdNotFound() throws Exception {
            when(jobDescriptionService.parse(999L)).thenReturn(null);

            mockMvc.perform(post("/api/v1/job-descriptions/999/parse"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST 全量重解析")
    class ReparseAll {

        @Test
        @DisplayName("正常重解析返回解析数量")
        void shouldReparseAll() throws Exception {
            when(jobDescriptionService.reparseAll()).thenReturn(3);

            mockMvc.perform(post("/api/v1/job-descriptions/reparse-all"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value(3));
        }
    }

    @Nested
    @DisplayName("DELETE 删除")
    class DeleteJd {

        @Test
        @DisplayName("正常删除返回 200")
        void shouldDeleteJd() throws Exception {
            when(jobDescriptionService.delete(1L)).thenReturn(true);

            mockMvc.perform(delete("/api/v1/job-descriptions/1"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("JD 不存在返回 404")
        void shouldReturn404WhenDeletingMissingJd() throws Exception {
            when(jobDescriptionService.delete(999L)).thenReturn(false);

            mockMvc.perform(delete("/api/v1/job-descriptions/999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("POST 批量更新岗位类型")
    class BatchUpdateJobType {

        @Test
        @DisplayName("正常批量更新返回更新数量")
        void shouldBatchUpdate() throws Exception {
            when(jobDescriptionService.batchUpdateJobType(anyString())).thenReturn(5);

            mockMvc.perform(post("/api/v1/job-descriptions/batch-update-job-type")
                            .param("jobType", "校招"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").value(5));
        }
    }
}
