package com.resumego.knowledge;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.resumego.knowledge.dto.KnowledgeImportResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class KnowledgeImportServiceTest {

    @TempDir
    Path tempDir;

    private static final String MD_CONTENT = "# 知识笔记\n\nTensorFlow 学习记录";
    private static final byte[] MD_BYTES = MD_CONTENT.getBytes(StandardCharsets.UTF_8);

    private KnowledgeRepository repository;
    private KnowledgeFileStore fileStore;
    private KnowledgeImportService service;

    @BeforeEach
    void setUp() {
        repository = mock(KnowledgeRepository.class);
        fileStore = new KnowledgeFileStore(tempDir);
        service = new KnowledgeImportService(repository, fileStore);
    }

    private MockMultipartFile mdFile() {
        return new MockMultipartFile("file", "笔记.md", "text/markdown", MD_BYTES);
    }

    private KnowledgeImportIds ids() {
        return new KnowledgeImportIds(10L, 20L, 30L);
    }

    private KnowledgeSourceFile existingSource(long documentId, String sha256) {
        return new KnowledgeSourceFile(20L, documentId, 1L, "笔记.md",
                "knowledge/sources/1/" + sha256 + ".md", null, "md", 4, sha256,
                "AVAILABLE", null, LocalDateTime.now(), LocalDateTime.now());
    }

    private KnowledgeDocument completedFileDocument(long documentId) {
        return new KnowledgeDocument(documentId, 1L, "本地文件知识", "FILE", "COMPLETED",
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void importsMarkdownAndCompletesWithStoredCopy() {
        when(repository.findSourceFileBySha(1L, sha(MD_BYTES))).thenReturn(Optional.empty());
        when(repository.insertImportRecords(eq(1L), any(), any())).thenReturn(ids());

        KnowledgeImportResponse response = service.importFile(mdFile());

        assertThat(response.documentId()).isEqualTo(10L);
        assertThat(response.duplicate()).isFalse();
        assertThat(response.processingStatus()).isEqualTo("COMPLETED");
        assertThat(response.errorCode()).isNull();
        verify(repository).completeImport(eq(10L), eq(20L), eq(30L), eq(1L), eq(MD_CONTENT));
        verify(repository, never()).failImport(anyLong(), anyLong(), anyLong(), any(), anyBoolean());

        // 原始副本落位且字节不变
        Path stored = fileStore.resolveStored(fileStore.sourceRelativePath(1L, sha(MD_BYTES), "md"));
        assertThat(stored).exists();
        assertThat(stored).hasBinaryContent(MD_BYTES);
    }

    @Test
    void importsTxtWithUtf8Text() {
        byte[] bytes = "纯文本内容".getBytes(StandardCharsets.UTF_8);
        when(repository.findSourceFileBySha(1L, sha(bytes))).thenReturn(Optional.empty());
        when(repository.insertImportRecords(eq(1L), any(), any())).thenReturn(ids());

        KnowledgeImportResponse response = service.importFile(
                new MockMultipartFile("file", "note.txt", "text/plain", bytes));

        assertThat(response.processingStatus()).isEqualTo("COMPLETED");
        verify(repository).completeImport(eq(10L), eq(20L), eq(30L), eq(1L), eq("纯文本内容"));
        Path stored = fileStore.resolveStored(fileStore.sourceRelativePath(1L, sha(bytes), "txt"));
        assertThat(stored).exists();
    }

    @Test
    void rejectsUnsupportedExtensionWithoutCreatingRecords() {
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", MD_BYTES);
        assertThatThrownBy(() -> service.importFile(file))
                .isInstanceOfSatisfying(KnowledgeImportException.class,
                        e -> assertThat(e.errorCode()).isEqualTo("UNSUPPORTED_TYPE"));
        verify(repository, never()).insertImportRecords(anyLong(), any(), any());
        verify(repository, never()).completeImport(anyLong(), anyLong(), anyLong(), anyLong(), any());
        assertThat(tempDir.resolve("knowledge")).doesNotExist();
    }

    @Test
    void rejectsMissingExtension() {
        MockMultipartFile file = new MockMultipartFile("file", "README", "text/plain", MD_BYTES);
        assertThatThrownBy(() -> service.importFile(file))
                .isInstanceOfSatisfying(KnowledgeImportException.class,
                        e -> assertThat(e.errorCode()).isEqualTo("UNSUPPORTED_TYPE"));
    }

    @Test
    void rejectsOversizedFileWithoutCreatingRecords() {
        byte[] big = new byte[(int) KnowledgeImportService.MAX_FILE_BYTES + 1];
        Arrays.fill(big, (byte) 'a');
        MockMultipartFile file = new MockMultipartFile("file", "big.md", "text/markdown", big);
        assertThatThrownBy(() -> service.importFile(file))
                .isInstanceOfSatisfying(KnowledgeImportException.class,
                        e -> assertThat(e.errorCode()).isEqualTo("FILE_TOO_LARGE"));
        verify(repository, never()).insertImportRecords(anyLong(), any(), any());
        assertThat(tempDir.resolve("knowledge")).doesNotExist();
    }

    @Test
    void rejectsPathCharactersInFilename() {
        MockMultipartFile file = new MockMultipartFile("file", "../etc/passwd.md", "text/markdown", MD_BYTES);
        assertThatThrownBy(() -> service.importFile(file))
                .isInstanceOfSatisfying(KnowledgeImportException.class,
                        e -> assertThat(e.errorCode()).isEqualTo("INVALID_FILENAME"));
        assertThat(tempDir.resolve("knowledge")).doesNotExist();
    }

    @Test
    void rejectsBackslashInFilename() {
        MockMultipartFile file = new MockMultipartFile("file", "a\\b.md", "text/markdown", MD_BYTES);
        assertThatThrownBy(() -> service.importFile(file))
                .isInstanceOfSatisfying(KnowledgeImportException.class,
                        e -> assertThat(e.errorCode()).isEqualTo("INVALID_FILENAME"));
    }

    @Test
    void rejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.md", "text/markdown", new byte[0]);
        assertThatThrownBy(() -> service.importFile(file))
                .isInstanceOfSatisfying(KnowledgeImportException.class,
                        e -> assertThat(e.errorCode()).isEqualTo("INVALID_FILENAME"));
    }

    @Test
    void marksInvalidUtf8AsFailedKeepingOriginalCopy() {
        byte[] bad = {(byte) 0xFF, (byte) 0xFE, 0x41};
        MockMultipartFile file = new MockMultipartFile("file", "坏文件.md", "text/markdown", bad);
        when(repository.findSourceFileBySha(1L, sha(bad))).thenReturn(Optional.empty());
        when(repository.insertImportRecords(eq(1L), any(), any())).thenReturn(ids());

        KnowledgeImportResponse response = service.importFile(file);

        assertThat(response.processingStatus()).isEqualTo("FAILED");
        assertThat(response.errorCode()).isEqualTo("INVALID_UTF8");
        verify(repository).failImport(10L, 20L, 30L, "INVALID_UTF8", true);
        verify(repository, never()).completeImport(anyLong(), anyLong(), anyLong(), anyLong(), any());
        // 已移动的原始副本保留
        Path stored = fileStore.resolveStored(fileStore.sourceRelativePath(1L, sha(bad), "md"));
        assertThat(stored).exists();
        assertThat(stored).hasBinaryContent(bad);
    }

    @Test
    void marksCopyFailureAsFailedKeepingStagedCopy() throws Exception {
        // sources/1 是普通文件 -> 目录创建失败 -> COPY_FAILED
        Path sources = tempDir.resolve("knowledge/sources/1");
        Files.createDirectories(sources.getParent());
        Files.write(sources, new byte[]{1});
        when(repository.findSourceFileBySha(1L, sha(MD_BYTES))).thenReturn(Optional.empty());
        when(repository.insertImportRecords(eq(1L), any(), any())).thenReturn(ids());

        KnowledgeImportResponse response = service.importFile(mdFile());

        assertThat(response.processingStatus()).isEqualTo("FAILED");
        assertThat(response.errorCode()).isEqualTo("COPY_FAILED");
        verify(repository).failImport(10L, 20L, 30L, "COPY_FAILED", false);
        // staging 副本保留
        Path stagingDir = tempDir.resolve("knowledge/staging");
        try (Stream<Path> stream = Files.list(stagingDir)) {
            assertThat(stream.findAny()).isPresent();
        }
    }

    @Test
    void returnsExistingDocumentForDuplicateFingerprint() {
        String sha = sha(MD_BYTES);
        when(repository.findSourceFileBySha(1L, sha)).thenReturn(Optional.of(existingSource(9L, sha)));
        when(repository.findById(1L, 9L)).thenReturn(Optional.of(completedFileDocument(9L)));

        KnowledgeImportResponse response = service.importFile(mdFile());

        assertThat(response.duplicate()).isTrue();
        assertThat(response.documentId()).isEqualTo(9L);
        assertThat(response.processingStatus()).isEqualTo("COMPLETED");
        verify(repository, never()).insertImportRecords(anyLong(), any(), any());
        assertThat(tempDir.resolve("knowledge")).doesNotExist();
    }

    @Test
    void concurrentDuplicateFallsBackToExistingDocumentAndCleansStaging() throws Exception {
        String sha = sha(MD_BYTES);
        when(repository.findSourceFileBySha(1L, sha))
                .thenReturn(Optional.empty(), Optional.of(existingSource(9L, sha)));
        when(repository.insertImportRecords(eq(1L), any(), any()))
                .thenThrow(new DuplicateKeyException("unique (user_id, sha256)"));
        when(repository.findById(1L, 9L)).thenReturn(Optional.of(completedFileDocument(9L)));

        KnowledgeImportResponse response = service.importFile(mdFile());

        assertThat(response.duplicate()).isTrue();
        assertThat(response.documentId()).isEqualTo(9L);
        verify(repository).insertImportRecords(eq(1L), any(), any());
        Path stagingDir = tempDir.resolve("knowledge/staging");
        try (Stream<Path> stream = Files.list(stagingDir)) {
            assertThat(stream.findAny()).isEmpty();
        }
    }

    @Test
    void logOutputContainsNoContentNameOrPath() {
        String content = "机密内容-勿泄露-" + System.nanoTime();
        String filename = "机密文件.md";
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        when(repository.findSourceFileBySha(1L, sha(bytes))).thenReturn(Optional.empty());
        when(repository.insertImportRecords(eq(1L), any(), any())).thenReturn(ids());

        ListAppender<ILoggingEvent> appender = attachAppender();
        try {
            service.importFile(new MockMultipartFile("file", filename, "text/markdown", bytes));
        } finally {
            detachAppender(appender);
        }
        String joined = appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .reduce("", (a, b) -> a + "|" + b);
        assertThat(joined).doesNotContain(content, filename, "knowledge/sources");
    }

    private ListAppender<ILoggingEvent> attachAppender() {
        ch.qos.logback.classic.Logger logger = logger();
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private void detachAppender(ListAppender<ILoggingEvent> appender) {
        logger().detachAppender(appender);
    }

    private ch.qos.logback.classic.Logger logger() {
        return ((LoggerContext) org.slf4j.LoggerFactory.getILoggerFactory())
                .getLogger("com.resumego.knowledge");
    }

    private String sha(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Test
    void usesSafeFileBasenameAsLocalTitleWithoutExtension() {
        when(repository.findSourceFileBySha(1L, sha(MD_BYTES))).thenReturn(Optional.empty());
        when(repository.insertImportRecords(eq(1L), eq("笔记"), any())).thenReturn(ids());

        service.importFile(mdFile());

        verify(repository).insertImportRecords(eq(1L), eq("笔记"), any());
    }

    @Test
    void normalizesAndTruncatesBasenameTo120Chars() {
        String longName = "长标题".repeat(60); // 180 字符
        byte[] bytes = longName.getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile("file", longName + ".md", "text/markdown", bytes);
        when(repository.findSourceFileBySha(1L, sha(bytes))).thenReturn(Optional.empty());
        when(repository.insertImportRecords(eq(1L), any(), any())).thenReturn(ids());

        service.importFile(file);

        var captor = ArgumentCaptor.forClass(String.class);
        verify(repository).insertImportRecords(eq(1L), captor.capture(), any());
        assertThat(captor.getValue()).hasSizeLessThanOrEqualTo(120);
        assertThat(captor.getValue()).isEqualTo("长标题".repeat(40)); // 120 字符截断
    }
}
