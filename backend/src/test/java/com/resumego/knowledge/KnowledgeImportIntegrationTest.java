package com.resumego.knowledge;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.resumego.knowledge.dto.KnowledgeImportResponse;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.jdbc.Sql;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Import({KnowledgeRepository.class, KnowledgeImportService.class,
        KnowledgeImportIntegrationTest.StoreConfig.class})
@Sql(scripts = "/sql/knowledge_schema.sql")
class KnowledgeImportIntegrationTest {

    @Autowired
    KnowledgeImportService service;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @TestConfiguration
    static class StoreConfig {
        static Path dataDir;

        @Bean
        KnowledgeFileStore knowledgeFileStore() {
            dataDir = Path.of(System.getProperty("java.io.tmpdir"), "io01-it-" + UUID.randomUUID());
            return new KnowledgeFileStore(dataDir);
        }
    }

    @AfterAll
    static void cleanup() throws IOException {
        if (StoreConfig.dataDir != null && Files.exists(StoreConfig.dataDir)) {
            try (var walk = Files.walk(StoreConfig.dataDir)) {
                walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                    try {
                        Files.deleteIfExists(p);
                    } catch (IOException ignored) {
                        // 尽力清理
                    }
                });
            }
        }
    }

    @Test
    void importsEndToEndPersistingDocumentSourceJobAndContent() {
        byte[] bytes = "# 端到端笔记\n\n导入成功".getBytes(StandardCharsets.UTF_8);
        String sha = sha(bytes);

        KnowledgeImportResponse response = service.importFile(
                new MockMultipartFile("file", "e2e.md", "text/markdown", bytes));

        assertThat(response.processingStatus()).isEqualTo("COMPLETED");
        assertThat(response.duplicate()).isFalse();

        Map<String, Object> doc = jdbcTemplate.queryForMap(
                "SELECT * FROM knowledge_documents WHERE id = ?", response.documentId());
        assertThat(String.valueOf(doc.get("processing_status"))).isEqualTo("COMPLETED");
        assertThat(String.valueOf(doc.get("source_type"))).isEqualTo("FILE");

        Map<String, Object> source = jdbcTemplate.queryForMap(
                "SELECT * FROM knowledge_source_files WHERE document_id = ?", response.documentId());
        assertThat(String.valueOf(source.get("availability"))).isEqualTo("AVAILABLE");
        assertThat(String.valueOf(source.get("sha256"))).isEqualTo(sha);
        assertThat(String.valueOf(source.get("stored_relative_path")))
                .isEqualTo("knowledge/sources/1/" + sha + ".md");
        assertThat(String.valueOf(source.get("original_name"))).isEqualTo("e2e.md");

        Map<String, Object> job = jdbcTemplate.queryForMap(
                "SELECT * FROM knowledge_import_jobs WHERE document_id = ?", response.documentId());
        assertThat(String.valueOf(job.get("job_status"))).isEqualTo("COMPLETED");
        assertThat(job.get("error_code")).isNull();

        Map<String, Object> content = jdbcTemplate.queryForMap(
                "SELECT * FROM knowledge_extracted_contents WHERE document_id = ?", response.documentId());
        assertThat(String.valueOf(content.get("content")))
                .isEqualTo(new String(bytes, StandardCharsets.UTF_8));

        Path stored = StoreConfig.dataDir.resolve("knowledge/sources/1/" + sha + ".md");
        assertThat(stored).exists();
        assertThat(stored).hasBinaryContent(bytes);
    }

    @Test
    void duplicateFingerprintReturnsExistingWithoutNewRows() {
        byte[] bytes = "去重笔记".getBytes(StandardCharsets.UTF_8);

        KnowledgeImportResponse first = service.importFile(
                new MockMultipartFile("file", "dup.md", "text/markdown", bytes));
        KnowledgeImportResponse second = service.importFile(
                new MockMultipartFile("file", "dup.md", "text/markdown", bytes));

        assertThat(first.duplicate()).isFalse();
        assertThat(second.duplicate()).isTrue();
        assertThat(second.documentId()).isEqualTo(first.documentId());
        Integer docs = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM knowledge_documents", Integer.class);
        Integer sources = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM knowledge_source_files", Integer.class);
        assertThat(docs).isEqualTo(1);
        assertThat(sources).isEqualTo(1);
    }

    @Test
    void invalidUtf8PersistsFailedJobAndKeepsCopy() {
        byte[] bad = {(byte) 0xFF, (byte) 0xFE, 0x41};

        KnowledgeImportResponse response = service.importFile(
                new MockMultipartFile("file", "坏.md", "text/markdown", bad));

        assertThat(response.processingStatus()).isEqualTo("FAILED");
        assertThat(response.errorCode()).isEqualTo("INVALID_UTF8");

        Map<String, Object> job = jdbcTemplate.queryForMap(
                "SELECT * FROM knowledge_import_jobs WHERE document_id = ?", response.documentId());
        assertThat(String.valueOf(job.get("job_status"))).isEqualTo("FAILED");
        assertThat(String.valueOf(job.get("error_code"))).isEqualTo("INVALID_UTF8");

        Map<String, Object> source = jdbcTemplate.queryForMap(
                "SELECT * FROM knowledge_source_files WHERE document_id = ?", response.documentId());
        assertThat(String.valueOf(source.get("availability"))).isEqualTo("AVAILABLE");
        Path stored = StoreConfig.dataDir.resolve(String.valueOf(source.get("stored_relative_path")));
        assertThat(stored).exists();
        assertThat(stored).hasBinaryContent(bad);
    }

    @Test
    void logsDoNotContainContentNameOrPath() {
        String content = "日志机密-" + System.nanoTime();
        String filename = "日志文件.md";
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

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
}
