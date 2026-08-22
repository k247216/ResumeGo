package com.resumego.knowledge;

import com.resumego.knowledge.dto.KnowledgeImportResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class KnowledgeImportControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    KnowledgeImportService importService;

    @MockBean
    KnowledgeService knowledgeService;

    @Test
    void importsSingleFileAndReturnsCreatedWithStatus() throws Exception {
        when(importService.importFile(any())).thenReturn(
                new KnowledgeImportResponse(7L, "FILE", "COMPLETED", false, null));

        mockMvc.perform(multipart("/api/v2/knowledge/imports")
                        .file(new MockMultipartFile("file", "notes.md", "text/markdown",
                                "hello".getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.documentId").value(7))
                .andExpect(jsonPath("$.data.sourceType").value("FILE"))
                .andExpect(jsonPath("$.data.processingStatus").value("COMPLETED"))
                .andExpect(jsonPath("$.data.duplicate").value(false));
    }

    @Test
    void duplicateImportReturnsOkWithExistingDocument() throws Exception {
        when(importService.importFile(any())).thenReturn(
                new KnowledgeImportResponse(7L, "FILE", "COMPLETED", true, null));

        mockMvc.perform(multipart("/api/v2/knowledge/imports")
                        .file(new MockMultipartFile("file", "notes.md", "text/markdown",
                                "hello".getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.duplicate").value(true));
    }

    @Test
    void rejectedImportMapsToBadRequestWithStableErrorCode() throws Exception {
        when(importService.importFile(any())).thenThrow(
                new KnowledgeImportException("UNSUPPORTED_TYPE", "仅支持 .md/.txt 文件"));

        mockMvc.perform(multipart("/api/v2/knowledge/imports")
                        .file(new MockMultipartFile("file", "doc.pdf", "application/pdf",
                                "x".getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("UNSUPPORTED_TYPE")));
    }

    @Test
    void oversizedImportMapsToBadRequestWithFileTooLargeCode() throws Exception {
        when(importService.importFile(any())).thenThrow(
                new KnowledgeImportException("FILE_TOO_LARGE", "文件不能超过 10 MiB"));

        mockMvc.perform(multipart("/api/v2/knowledge/imports")
                        .file(new MockMultipartFile("file", "big.md", "text/markdown",
                                "x".getBytes(StandardCharsets.UTF_8))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("FILE_TOO_LARGE")));
    }
}
