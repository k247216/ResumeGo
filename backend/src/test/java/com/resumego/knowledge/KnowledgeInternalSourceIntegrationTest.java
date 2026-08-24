package com.resumego.knowledge;

import com.resumego.knowledge.dto.KnowledgeManagedSourceResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JdbcTest
@Import({KnowledgeRepository.class, KnowledgeInternalSourceService.class,
        KnowledgeInternalSourceIntegrationTest.StoreConfig.class})
@Sql(scripts = "/sql/knowledge_schema.sql")
class KnowledgeInternalSourceIntegrationTest {

    @Autowired
    KnowledgeRepository repository;

    @Autowired
    KnowledgeInternalSourceService service;

    @TestConfiguration
    static class StoreConfig {
        static Path dataDir;

        @Bean
        KnowledgeFileStore knowledgeFileStore() {
            dataDir = Path.of(System.getProperty("java.io.tmpdir"), "io02-it-" + UUID.randomUUID());
            return new KnowledgeFileStore(dataDir);
        }
    }

    @BeforeEach
    void clean() throws Exception {
        if (StoreConfig.dataDir != null && Files.exists(StoreConfig.dataDir)) {
            try (var walk = Files.walk(StoreConfig.dataDir)) {
                walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                    try { Files.deleteIfExists(p); } catch (Exception ignored) { }
                });
            }
        }
    }

    private long availableFileDocument(String sha, String content) throws Exception {
        KnowledgeImportIds ids = repository.insertImportRecords(1L, "文件", new KnowledgeSourceFileDraft(
                "a.md", "knowledge/sources/1/" + sha + ".md", "md", 10, sha, "STAGED", null));
        Path target = StoreConfig.dataDir.resolve("knowledge/sources/1/" + sha + ".md");
        Files.createDirectories(target.getParent());
        Files.write(target, content.getBytes(StandardCharsets.UTF_8));
        repository.failImport(ids.documentId(), ids.sourceFileId(), ids.importJobId(), "EXTRACTION_FAILED", true);
        repository.completeImportRetry(ids.documentId(), ids.sourceFileId(), ids.importJobId(), 1L, content);
        return ids.documentId();
    }

    @Test
    void returnsManagedPathForAvailableFile() throws Exception {
        long docId = availableFileDocument("sha-ok", "可用文件正文");
        KnowledgeManagedSourceResponse response = service.managedSource(docId);
        assertThat(response.relativePath()).isEqualTo("knowledge/sources/1/sha-ok.md");
    }

    @Test
    void marksAvailabilityMissingWhenFileDisappears() throws Exception {
        long docId = availableFileDocument("sha-gone", "将消失");
        Files.deleteIfExists(StoreConfig.dataDir.resolve("knowledge/sources/1/sha-gone.md"));

        assertThatThrownBy(() -> service.managedSource(docId))
                .isInstanceOf(ManagedSourceException.class)
                .extracting(e -> ((ManagedSourceException) e).code())
                .isEqualTo("SOURCE_MISSING");
        KnowledgeSourceFile source = repository.findSourceFileByDocument(1L, docId).orElseThrow();
        assertThat(source.availability()).isEqualTo("MISSING");
        // 文档与正文保留
        assertThat(repository.findById(1L, docId)).isPresent();
        assertThat(repository.findExtractedContentByDocument(1L, docId)).isPresent();
    }
}
