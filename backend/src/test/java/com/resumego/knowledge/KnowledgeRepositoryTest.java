package com.resumego.knowledge;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JdbcTest
@Import(KnowledgeRepository.class)
@Sql(scripts = "/sql/knowledge_schema.sql")
class KnowledgeRepositoryTest {

    @Autowired KnowledgeRepository repository;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void createsUserScopedNoteMetadataWithNormalizedTitle() {
        long id = repository.insertDocument(1L, "TensorFlow 学习笔记", "NOTE", "NOT_STARTED");
        assertThat(id).isPositive();
        Optional<KnowledgeDocument> doc = repository.findById(1L, id);
        assertThat(doc).isPresent();
        assertThat(doc.get().title()).isEqualTo("TensorFlow 学习笔记");
        assertThat(doc.get().sourceType()).isEqualTo("NOTE");
        assertThat(doc.get().processingStatus()).isEqualTo("NOT_STARTED");
    }

    @Test
    void listsCurrentUserDocumentsNewestFirstAndIsolatesOtherUsers() {
        long a = repository.insertDocument(1L, "笔记甲", "NOTE", "NOT_STARTED");
        long b = repository.insertDocument(1L, "笔记乙", "NOTE", "NOT_STARTED");
        repository.insertDocument(2L, "他人笔记", "NOTE", "NOT_STARTED");

        List<KnowledgeDocument> mine = repository.listByUser(1L);
        assertThat(mine).extracting(KnowledgeDocument::id).contains(a, b);
        assertThat(mine).extracting(KnowledgeDocument::title).doesNotContain("他人笔记");

        assertThat(repository.findById(1L, b).orElseThrow().id()).isEqualTo(b);
        assertThat(repository.findById(1L, 999L)).isEmpty();
        // 他人记录对 user1 不可见
        assertThat(repository.findById(1L, a)).isPresent(); // own
    }

    @Test
    void ordersByUpdatedAtDescThenIdDesc() {
        long first = repository.insertDocument(1L, "较早", "NOTE", "NOT_STARTED");
        long second = repository.insertDocument(1L, "较新", "NOTE", "NOT_STARTED");
        List<KnowledgeDocument> list = repository.listByUser(1L);
        assertThat(list.get(0).id()).isEqualTo(second);
        assertThat(list.get(1).id()).isEqualTo(first);
        assertThat(first).isLessThan(second);
    }

    @Test
    void insertImportRecordsCreatesDocumentSourceAndJobAndFindsBySha() {
        KnowledgeImportIds ids = repository.insertImportRecords(1L, "本地文件知识",
                new KnowledgeSourceFileDraft("笔记.md", "knowledge/sources/1/abc.md", "md", 4, "abc123", "STAGED"));
        assertThat(ids.documentId()).isPositive();
        assertThat(ids.sourceFileId()).isPositive();
        assertThat(ids.importJobId()).isPositive();

        Optional<KnowledgeSourceFile> source = repository.findSourceFileBySha(1L, "abc123");
        assertThat(source).isPresent();
        assertThat(source.get().documentId()).isEqualTo(ids.documentId());
        assertThat(source.get().availability()).isEqualTo("STAGED");
        assertThat(source.get().storedRelativePath()).isEqualTo("knowledge/sources/1/abc.md");
        // 跨用户隔离：user2 查不到 user1 的 source
        assertThat(repository.findSourceFileBySha(2L, "abc123")).isEmpty();
    }

    @Test
    void insertImportRecordsRejectsSameShaForSameUserButAllowsOtherUser() {
        repository.insertImportRecords(1L, "本地文件知识",
                new KnowledgeSourceFileDraft("a.md", "knowledge/sources/1/a.md", "md", 4, "sha1", "STAGED"));
        assertThatThrownBy(() -> repository.insertImportRecords(1L, "本地文件知识",
                new KnowledgeSourceFileDraft("b.md", "knowledge/sources/1/b.md", "md", 4, "sha1", "STAGED")))
                .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
        // 相同 sha 不同用户允许
        repository.insertImportRecords(2L, "本地文件知识",
                new KnowledgeSourceFileDraft("c.md", "knowledge/sources/2/c.md", "md", 4, "sha1", "STAGED"));
        assertThat(repository.findSourceFileBySha(2L, "sha1")).isPresent();
    }

    @Test
    void completeImportPersistsAvailableSourceContentAndCompletedStates() {
        KnowledgeImportIds ids = repository.insertImportRecords(1L, "本地文件知识",
                new KnowledgeSourceFileDraft("笔记.md", "knowledge/sources/1/x.md", "md", 4, "sha9", "STAGED"));

        repository.completeImport(ids.documentId(), ids.sourceFileId(), ids.importJobId(), 1L, "正文内容");

        assertThat(repository.findById(1L, ids.documentId()).orElseThrow().processingStatus())
                .isEqualTo("COMPLETED");
        Optional<KnowledgeExtractedContent> content = repository.findExtractedContentByDocument(1L, ids.documentId());
        assertThat(content).isPresent();
        assertThat(content.get().content()).isEqualTo("正文内容");
        assertThat(repository.findExtractedContentByDocument(2L, ids.documentId())).isEmpty();
        String availability = jdbcTemplate.queryForObject(
                "SELECT availability FROM knowledge_source_files WHERE id = ?", String.class, ids.sourceFileId());
        assertThat(availability).isEqualTo("AVAILABLE");
    }

    @Test
    void failImportPersistsErrorCodeAndKeepsStagedWhenCopyMissing() {
        KnowledgeImportIds ids = repository.insertImportRecords(1L, "本地文件知识",
                new KnowledgeSourceFileDraft("笔记.md", "knowledge/sources/1/y.md", "md", 4, "sha8", "STAGED"));

        repository.failImport(ids.documentId(), ids.sourceFileId(), ids.importJobId(), "COPY_FAILED", false);

        assertThat(repository.findById(1L, ids.documentId()).orElseThrow().processingStatus())
                .isEqualTo("FAILED");
        Map<String, Object> job = jdbcTemplate.queryForMap(
                "SELECT * FROM knowledge_import_jobs WHERE id = ?", ids.importJobId());
        assertThat(String.valueOf(job.get("job_status"))).isEqualTo("FAILED");
        assertThat(String.valueOf(job.get("error_code"))).isEqualTo("COPY_FAILED");
        String availability = jdbcTemplate.queryForObject(
                "SELECT availability FROM knowledge_source_files WHERE id = ?", String.class, ids.sourceFileId());
        assertThat(availability).isEqualTo("STAGED");

        // 副本已落位时 failImport 保持 AVAILABLE
        repository.failImport(ids.documentId(), ids.sourceFileId(), ids.importJobId(), "INVALID_UTF8", true);
        String availability2 = jdbcTemplate.queryForObject(
                "SELECT availability FROM knowledge_source_files WHERE id = ?", String.class, ids.sourceFileId());
        assertThat(availability2).isEqualTo("AVAILABLE");
    }

    @Test
    void importJobStatusTransitionsSetStartAndFinishTimestamps() {
        KnowledgeImportIds ids = repository.insertImportRecords(1L, "本地文件知识",
                new KnowledgeSourceFileDraft("笔记.md", "knowledge/sources/1/z.md", "md", 4, "sha7", "STAGED"));

        repository.updateImportJobStatus(ids.importJobId(), "RUNNING", null);
        Map<String, Object> running = jdbcTemplate.queryForMap(
                "SELECT * FROM knowledge_import_jobs WHERE id = ?", ids.importJobId());
        assertThat(running.get("started_at")).isNotNull();
        assertThat(running.get("finished_at")).isNull();

        repository.updateImportJobStatus(ids.importJobId(), "COMPLETED", null);
        Map<String, Object> done = jdbcTemplate.queryForMap(
                "SELECT * FROM knowledge_import_jobs WHERE id = ?", ids.importJobId());
        assertThat(String.valueOf(done.get("job_status"))).isEqualTo("COMPLETED");
        assertThat(done.get("finished_at")).isNotNull();
    }

    @Test
    void categoriesAreUserScopedAndNormalizedNameUniquePerUser() {
        long c1 = repository.insertCategory(1L, "求职", "求职");
        assertThat(c1).isPositive();
        assertThat(repository.findCategoryByNormalizedName(1L, "求职")).isPresent();
        // 用户隔离：user2 查不到 user1 的分类
        assertThat(repository.findCategoryByNormalizedName(2L, "求职")).isEmpty();
        assertThat(repository.findCategoryById(1L, c1)).isPresent();
        assertThat(repository.findCategoryById(2L, c1)).isEmpty();

        assertThatThrownBy(() -> repository.insertCategory(1L, "求职笔记", "求职"))
                .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
        // 相同 normalized 不同用户允许
        repository.insertCategory(2L, "求职", "求职");
        assertThat(repository.listCategories(1L)).extracting(KnowledgeCategory::normalizedName).contains("求职");
    }

    @Test
    void tagsAreUserScopedAndUniquePerUser() {
        long t1 = repository.insertTag(1L, "机器学习", "机器学习");
        assertThat(repository.findTagByNormalizedName(1L, "机器学习")).isPresent();
        assertThat(repository.findTagByNormalizedName(2L, "机器学习")).isEmpty();
        assertThatThrownBy(() -> repository.insertTag(1L, "机器学习新", "机器学习"))
                .isInstanceOf(org.springframework.dao.DuplicateKeyException.class);
        assertThat(repository.findTagById(1L, t1)).isPresent();
        assertThat(repository.findTagById(2L, t1)).isEmpty();
    }

    @Test
    void documentCategoryIsSinglePerDocumentAndReplaced() {
        long docId = repository.insertDocument(1L, "笔记", "NOTE", "NOT_STARTED");
        long cat1 = repository.insertCategory(1L, "求职", "求职");
        long cat2 = repository.insertCategory(1L, "技能", "技能");

        repository.setDocumentCategory(1L, docId, cat1);
        Long current = jdbcTemplate.queryForObject(
                "SELECT category_id FROM knowledge_document_categories WHERE document_id = ?", Long.class, docId);
        assertThat(current).isEqualTo(cat1);

        // 重新设置替换旧分类：每文档最多一个
        repository.setDocumentCategory(1L, docId, cat2);
        Long after = jdbcTemplate.queryForObject(
                "SELECT category_id FROM knowledge_document_categories WHERE document_id = ?", Long.class, docId);
        assertThat(after).isEqualTo(cat2);
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM knowledge_document_categories WHERE document_id = ?", Integer.class, docId);
        assertThat(count).isEqualTo(1);

        repository.removeDocumentCategory(1L, docId, cat2);
        Integer remaining = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM knowledge_document_categories WHERE document_id = ?", Integer.class, docId);
        assertThat(remaining).isZero();
    }

    @Test
    void documentTagAssociationsAreIdempotent() {
        long docId = repository.insertDocument(1L, "笔记", "NOTE", "NOT_STARTED");
        long tag1 = repository.insertTag(1L, "机器学习", "机器学习");
        long tag2 = repository.insertTag(1L, "面试", "面试");

        repository.addDocumentTag(1L, docId, tag1);
        repository.addDocumentTag(1L, docId, tag1); // 重复关联幂等
        repository.addDocumentTag(1L, docId, tag2);

        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM knowledge_document_tags WHERE document_id = ?", Integer.class, docId);
        assertThat(count).isEqualTo(2);

        repository.removeDocumentTag(1L, docId, tag1);
        Integer after = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM knowledge_document_tags WHERE document_id = ?", Integer.class, docId);
        assertThat(after).isEqualTo(1);
    }
}
