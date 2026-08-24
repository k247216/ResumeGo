package com.resumego.knowledge;

import com.resumego.knowledge.dto.KnowledgeDocumentResponse;
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
@Import({KnowledgeRepository.class, KnowledgeService.class})
@Sql(scripts = "/sql/knowledge_schema.sql")
class KnowledgeBrowseIntegrationTest {

    @Autowired
    KnowledgeRepository repository;

    @Autowired
    KnowledgeService service;

    @Autowired
    JdbcTemplate jdbcTemplate;

    private long docA;
    private long docB;
    private long docC;
    private long folderRoot;
    private long folderChild;

    @BeforeEach
    void setUp() {
        docA = repository.insertDocument(1L, "文档甲", "NOTE", "COMPLETED");
        docB = repository.insertDocument(1L, "文档乙", "NOTE", "COMPLETED");
        docC = repository.insertDocument(1L, "文档丙", "NOTE", "COMPLETED");
        folderRoot = repository.insertCategoryWithParent(1L, "根", "根", null);
        folderChild = repository.insertCategoryWithParent(1L, "子", "子", folderRoot);
        long tag = repository.insertTag(1L, "标签", "标签");
        repository.setDocumentCategory(1L, docA, folderRoot);
        repository.setDocumentCategory(1L, docB, folderChild);
        repository.addDocumentTag(1L, docB, tag);
    }

    @Test
    void allDocumentsWithoutFilter() {
        List<KnowledgeDocumentResponse> all = service.list(null, null, false);
        assertThat(all).extracting(KnowledgeDocumentResponse::id)
                .containsExactlyInAnyOrder(docA, docB, docC);
    }

    @Test
    void folderBrowseIncludesDescendantsByDefault() {
        List<KnowledgeDocumentResponse> root = service.list(folderRoot, null, true);
        assertThat(root).extracting(KnowledgeDocumentResponse::id)
                .containsExactlyInAnyOrder(docA, docB); // 根 + 子文件夹后代
    }

    @Test
    void folderBrowseExcludesDescendantsWhenFalse() {
        List<KnowledgeDocumentResponse> root = service.list(folderRoot, null, false);
        assertThat(root).extracting(KnowledgeDocumentResponse::id).containsExactly(docA);
    }

    @Test
    void tagBrowseFiltersByRealAssociation() {
        long tag = jdbcTemplate.queryForObject(
                "SELECT id FROM knowledge_tags WHERE name = '标签'", Long.class);
        List<KnowledgeDocumentResponse> tagged = service.list(null, tag, false);
        assertThat(tagged).extracting(KnowledgeDocumentResponse::id).containsExactly(docB);
    }

    @Test
    void browseIsIsolatedPerUser() {
        repository.insertDocument(2L, "他人文档", "NOTE", "COMPLETED");
        assertThat(service.list(null, null, false))
                .extracting(KnowledgeDocumentResponse::title).noneMatch(t -> t.contains("他人"));
    }
}
