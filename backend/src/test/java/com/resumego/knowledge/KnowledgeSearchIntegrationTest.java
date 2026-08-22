package com.resumego.knowledge;

import com.resumego.knowledge.dto.KnowledgeDocumentClassificationResponse;
import com.resumego.knowledge.dto.KnowledgeSearchItemResponse;
import com.resumego.knowledge.dto.KnowledgeTagResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Import({KnowledgeRepository.class, KnowledgeClassificationService.class})
@Sql(scripts = "/sql/knowledge_schema.sql")
class KnowledgeSearchIntegrationTest {

    @Autowired
    KnowledgeRepository repository;

    @Autowired
    KnowledgeClassificationService service;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private long tensorflowNote;
    private long interviewDoc;
    private long pendingDoc;
    private long wildcardDoc;

    @BeforeEach
    void setUp() {
        // user1 文档
        tensorflowNote = repository.insertDocument(1L, "TensorFlow 学习笔记", "NOTE", "NOT_STARTED");
        KnowledgeImportIds ids = repository.insertImportRecords(1L, "面试准备", new KnowledgeSourceFileDraft(
                "面试.md", "knowledge/sources/1/x.md", "md", 10, "sha-x", "STAGED"));
        interviewDoc = ids.documentId();
        repository.completeImport(ids.documentId(), ids.sourceFileId(), ids.importJobId(), 1L,
                "# 面试准备\n\n自我介绍\n项目经历 TensorFlow 部署\n复盘");
        pendingDoc = repository.insertImportRecords(1L, "Python 脚本", new KnowledgeSourceFileDraft(
                "脚本.md", "knowledge/sources/1/y.md", "md", 10, "sha-y", "STAGED")).documentId();
        wildcardDoc = repository.insertDocument(1L, "100%_成功记录", "NOTE", "NOT_STARTED");
    }

    @Test
    void titleMatchesAllDocumentsWithCaseInsensitiveEnglish() {
        List<KnowledgeSearchItemResponse> results = service.search("tensorflow", null, null);
        assertThat(results).isNotEmpty();
        assertThat(results).anyMatch(r -> r.document().id() == tensorflowNote && "TITLE".equals(r.matchedField()));
        assertThat(results).anyMatch(r -> r.document().id() == interviewDoc && "CONTENT".equals(r.matchedField()));
    }

    @Test
    void contentOnlyMatchesCompletedDocuments() {
        // pending 文档即使有正文行也不能伪造命中
        jdbcTemplate.update("""
                INSERT INTO knowledge_extracted_contents (document_id, user_id, content)
                VALUES (?, 1, 'Python 脚本 部署 复盘')
                """, pendingDoc);
        List<KnowledgeSearchItemResponse> results = service.search("复盘", null, null);
        assertThat(results).isNotEmpty();
        assertThat(results).noneMatch(r -> r.document().id() == pendingDoc);
        assertThat(results).anyMatch(r -> r.document().id() == interviewDoc
                && "CONTENT".equals(r.matchedField())
                && r.lineNumber() != null
                && r.snippet().contains("复盘"));
    }

    @Test
    void wildcardAndEscapeCharsMatchLiterally() {
        long bangDoc = repository.insertDocument(1L, "真棒!达成", "NOTE", "NOT_STARTED");
        long backslashDoc = repository.insertDocument(1L, "路径 C:\\Temp", "NOTE", "NOT_STARTED");

        List<KnowledgeSearchItemResponse> results = service.search("100%_成功", null, null);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).document().id()).isEqualTo(wildcardDoc);

        // 单独的 % 只命中确实包含字面 % 的标题
        List<KnowledgeSearchItemResponse> percent = service.search("%", null, null);
        assertThat(percent).isNotEmpty();
        assertThat(percent).allMatch(r -> r.document().title().contains("%"));

        // 单独的 _ 只命中字面 _
        List<KnowledgeSearchItemResponse> underscore = service.search("_", null, null);
        assertThat(underscore).isNotEmpty();
        assertThat(underscore).allMatch(r -> r.document().title().contains("_"));

        // 转义符 ! 本身可作字面字符搜索
        List<KnowledgeSearchItemResponse> bang = service.search("真棒!达成", null, null);
        assertThat(bang).hasSize(1);
        assertThat(bang.get(0).document().id()).isEqualTo(bangDoc);
        List<KnowledgeSearchItemResponse> loneBang = service.search("!", null, null);
        assertThat(loneBang).isNotEmpty();
        assertThat(loneBang).allMatch(r -> r.document().title().contains("!"));

        // 反斜杠作为普通字面字符
        List<KnowledgeSearchItemResponse> backslash = service.search("C:\\Temp", null, null);
        assertThat(backslash).isNotEmpty();
        assertThat(backslash).anyMatch(r -> r.document().id() == backslashDoc);
    }

    @Test
    void readsDocumentClassificationWithOwnedRelations() {
        long categoryId = repository.insertCategory(1L, "求职", "求职");
        long tagA = repository.insertTag(1L, "机器学习", "机器学习");
        long tagB = repository.insertTag(1L, "面试", "面试");
        repository.setDocumentCategory(1L, tensorflowNote, categoryId);
        repository.addDocumentTag(1L, tensorflowNote, tagB);
        repository.addDocumentTag(1L, tensorflowNote, tagA);

        KnowledgeDocumentClassificationResponse result = service.getDocumentClassification(tensorflowNote);
        assertThat(result.category()).isNotNull();
        assertThat(result.category().name()).isEqualTo("求职");
        assertThat(result.tags()).extracting(KnowledgeTagResponse::name).containsExactly("机器学习", "面试");

        KnowledgeDocumentClassificationResponse empty = service.getDocumentClassification(wildcardDoc);
        assertThat(empty.category()).isNull();
        assertThat(empty.tags()).isEmpty();
    }

    @Test
    void combinedCategoryAndTagFiltersRestrictResults() {
        long categoryId = repository.insertCategory(1L, "求职", "求职");
        long tagId = repository.insertTag(1L, "机器学习", "机器学习");
        long otherTag = repository.insertTag(1L, "面试", "面试");
        repository.setDocumentCategory(1L, tensorflowNote, categoryId);
        repository.addDocumentTag(1L, tensorflowNote, tagId);
        repository.addDocumentTag(1L, interviewDoc, otherTag);

        List<KnowledgeSearchItemResponse> both = service.search("TensorFlow", categoryId, tagId);
        assertThat(both).hasSize(1);
        assertThat(both.get(0).document().id()).isEqualTo(tensorflowNote);

        // 只按标签过滤：interviewDoc 命中但被 otherTag 过滤后仅剩有该标签的文档
        List<KnowledgeSearchItemResponse> byTag = service.search("TensorFlow", null, otherTag);
        assertThat(byTag).hasSize(1);
        assertThat(byTag.get(0).document().id()).isEqualTo(interviewDoc);
    }

    @Test
    void emptyResultForMissingKeyword() {
        assertThat(service.search("不存在的词", null, null)).isEmpty();
    }

    @Test
    void searchIsIsolatedPerUser() {
        repository.insertDocument(2L, "他人的 TensorFlow 笔记", "NOTE", "NOT_STARTED");
        List<KnowledgeSearchItemResponse> results = service.search("TensorFlow", null, null);
        assertThat(results).noneMatch(r -> r.document().title().contains("他人的"));
    }

    @Test
    void ordersByUpdatedAtDescThenIdDesc() {
        jdbcTemplate.update("UPDATE knowledge_documents SET updated_at = DATEADD('SECOND', -10, CURRENT_TIMESTAMP) WHERE id = ?",
                tensorflowNote);
        List<KnowledgeSearchItemResponse> results = service.search("TensorFlow", null, null);
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).document().id()).isEqualTo(interviewDoc);
        assertThat(results.get(0).document().processingStatus()).isEqualTo("COMPLETED");
    }
}
