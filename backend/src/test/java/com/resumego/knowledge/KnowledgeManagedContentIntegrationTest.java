package com.resumego.knowledge;

import com.resumego.knowledge.dto.KnowledgeDocumentResponse;
import com.resumego.knowledge.dto.KnowledgeSearchItemResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.PlatformTransactionManager;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JdbcTest
@Import({KnowledgeRepository.class, KnowledgeManagedContentService.class,
        KnowledgeService.class, KnowledgeClassificationService.class,
        KnowledgeManagedContentIntegrationTest.StoreConfig.class})
@Sql(scripts = "/sql/knowledge_schema.sql")
class KnowledgeManagedContentIntegrationTest {

    @Autowired
    KnowledgeRepository repository;

    @Autowired
    KnowledgeManagedContentService managedContent;

    @Autowired
    KnowledgeService service;

    @Autowired
    KnowledgeClassificationService classification;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @TestConfiguration
    static class StoreConfig {
        static Path dataDir;

        @Bean
        KnowledgeFileStore knowledgeFileStore() {
            dataDir = Path.of(System.getProperty("java.io.tmpdir"), "be06-it-" + UUID.randomUUID());
            return new KnowledgeFileStore(dataDir);
        }

        @Bean
        KnowledgeManagedContentService managedContent(KnowledgeRepository repository,
                                                      KnowledgeFileStore store,
                                                      PlatformTransactionManager txManager) {
            return new KnowledgeManagedContentService(repository, store, txManager);
        }
    }

    private long mdDocId;
    private long txtDocId;
    private long noteDocId;

    @BeforeEach
    void setUp() throws Exception {
        noteDocId = repository.createNoteWithEmptyContent(1L, "笔记甲");
        mdDocId = createFileDocument("md", "old-sha", "# 旧标题", "Markdown 文档");
        String txtSha = sha256Of("纯文本");
        txtDocId = createFileDocument("txt", txtSha, "纯文本", "TXT 文档");
    }

    private String sha256Of(String content) {
        try {
            return java.util.HexFormat.of().formatHex(
                    java.security.MessageDigest.getInstance("SHA-256")
                            .digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private long createFileDocument(String ext, String sha, String content, String title) throws Exception {
        KnowledgeImportIds ids = repository.insertImportRecords(1L, title, new KnowledgeSourceFileDraft(
                "a." + ext, "knowledge/sources/1/" + sha + "." + ext, ext, content.getBytes(StandardCharsets.UTF_8).length,
                sha, "STAGED", null));
        Path target = StoreConfig.dataDir.resolve("knowledge/sources/1/" + sha + "." + ext);
        Files.createDirectories(target.getParent());
        Files.write(target, content.getBytes(StandardCharsets.UTF_8));
        repository.failImport(ids.documentId(), ids.sourceFileId(), ids.importJobId(), "EXTRACTION_FAILED", true);
        repository.completeImportRetry(ids.documentId(), ids.sourceFileId(), ids.importJobId(), 1L, content);
        return ids.documentId();
    }

    @Test
    void sourceExtensionAndSourceFileReflectRealTypeInListAndDetail() {
        List<KnowledgeDocumentResponse> all = service.list();
        assertThat(all).filteredOn(d -> d.id() == noteDocId).first()
                .satisfies(d -> {
                    assertThat(d.sourceExtension()).isNull();
                    assertThat(d.sourceFile()).isNull();
                });
        assertThat(all).filteredOn(d -> d.id() == mdDocId).first()
                .satisfies(d -> {
                    assertThat(d.sourceExtension()).isEqualTo("md");
                    assertThat(d.sourceFile()).isEqualTo("a.md");
                });
        assertThat(all).filteredOn(d -> d.id() == txtDocId).first()
                .satisfies(d -> {
                    assertThat(d.sourceExtension()).isEqualTo("txt");
                    assertThat(d.sourceFile()).isEqualTo("a.txt");
                });
    }

    @Test
    void markdownSaveReplacesManagedCopyAndSearchMatchesImmediately() throws Exception {
        String newContent = "# 新标题\n\n- 更新后的项目经历";
        managedContent.saveContent(mdDocId, newContent);

        // 受管文件字节更新：目标为 newHash 对应路径（hash 命名不变量）
        String newSha = sha256Of(newContent);
        Path newTarget = StoreConfig.dataDir.resolve("knowledge/sources/1/" + newSha + ".md");
        assertThat(Files.readString(newTarget)).isEqualTo(newContent);
        // 旧 hash 路径文件已释放
        assertThat(StoreConfig.dataDir.resolve("knowledge/sources/1/old-sha.md")).doesNotExist();
        // 正文与元数据同步（stored path 指向新 hash）
        assertThat(service.getContent(mdDocId).content()).isEqualTo(newContent);
        KnowledgeSourceFile source = repository.findSourceFileByDocument(1L, mdDocId).orElseThrow();
        assertThat(source.storedRelativePath()).isEqualTo("knowledge/sources/1/" + newSha + ".md");
        assertThat(source.sizeBytes()).isEqualTo(newContent.getBytes(StandardCharsets.UTF_8).length);
        // 搜索立即命中
        List<KnowledgeSearchItemResponse> results = classification.search("更新后的项目经历", null, null, false);
        assertThat(results).anyMatch(r -> r.document().id() == mdDocId && "CONTENT".equals(r.matchedField()));
        // 旧内容可再次导入（旧 hash 路径已释放，唯一约束不冲突）
        createFileDocument("md", sha256Of("# 旧标题"), "# 旧标题", "再次导入的旧文档");
    }

    @Test
    void txtRejectedAndNoteEmptyContentReadable() {
        assertThatThrownBy(() -> managedContent.saveContent(txtDocId, "x"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("NOT_EDITABLE");
        // NOTE 创建即有空正文且 COMPLETED
        assertThat(service.getContent(noteDocId).content()).isEmpty();
        assertThat(repository.findById(1L, noteDocId).orElseThrow().processingStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void hashConflictRejectedKeepingBothDocuments() throws Exception {
        // 新内容 hash 与 txtDocId 的内容相同（不同 sha 前缀但内容一致则 sha 相同）→ 冲突
        String colliding = "纯文本";
        // 先确认 colliding 与 txt 内容相同 → sha 相同 → 与 txtDoc 冲突（不同文档）
        assertThatThrownBy(() -> managedContent.saveContent(mdDocId, colliding))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("HASH_CONFLICT");
        // 两个文档及文件保持不变
        assertThat(repository.findById(1L, mdDocId)).isPresent();
        assertThat(repository.findById(1L, txtDocId)).isPresent();
        assertThat(Files.readString(StoreConfig.dataDir.resolve("knowledge/sources/1/old-sha.md")))
                .isEqualTo("# 旧标题");
    }
}
