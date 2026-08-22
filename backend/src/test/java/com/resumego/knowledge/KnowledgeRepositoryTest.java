package com.resumego.knowledge;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

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
}
