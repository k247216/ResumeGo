package com.resumego.knowledge;

import com.resumego.knowledge.dto.CreateKnowledgeCategoryRequest;
import com.resumego.knowledge.dto.KnowledgeCategoryNodeResponse;
import com.resumego.knowledge.dto.KnowledgeSearchItemResponse;
import com.resumego.knowledge.dto.UpdateKnowledgeCategoryRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JdbcTest
@Import({KnowledgeRepository.class, KnowledgeClassificationService.class})
@Sql(scripts = "/sql/knowledge_schema.sql")
class KnowledgeHierarchyIntegrationTest {

    @Autowired
    KnowledgeRepository repository;

    @Autowired
    KnowledgeClassificationService service;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        // user1 文档
        repository.insertDocument(1L, "文档甲", "NOTE", "NOT_STARTED");
        repository.insertDocument(1L, "文档乙", "NOTE", "NOT_STARTED");
        repository.insertDocument(1L, "文档丙", "NOTE", "NOT_STARTED");
        repository.insertDocument(2L, "他人文档", "NOTE", "NOT_STARTED");
    }

    private long docId(int index) {
        return jdbcTemplate.queryForObject(
                "SELECT id FROM knowledge_documents WHERE user_id = 1 ORDER BY id LIMIT 1 OFFSET " + (index - 1),
                Long.class);
    }

    @Test
    void createsTreeAndComputesDepthAndCountsCorrectly() {
        long a = service.createCategory(new CreateKnowledgeCategoryRequest("A", null)).response().id();
        long b = service.createCategory(new CreateKnowledgeCategoryRequest("B", a)).response().id();
        long c = service.createCategory(new CreateKnowledgeCategoryRequest("C", b)).response().id();
        long d = service.createCategory(new CreateKnowledgeCategoryRequest("D", c)).response().id();

        repository.setDocumentCategory(1L, docId(1), a);
        repository.setDocumentCategory(1L, docId(2), c);

        List<KnowledgeCategoryNodeResponse> nodes = service.listCategoryTree();
        assertThat(nodes).hasSize(4);
        assertThat(nodes).filteredOn(n -> n.id() == a).first().satisfies(n -> {
            assertThat(n.parentId()).isNull();
            assertThat(n.depth()).isZero();
            assertThat(n.directDocumentCount()).isEqualTo(1);
            assertThat(n.descendantDocumentCount()).isEqualTo(2); // 自身 1 + c 的 1
        });
        assertThat(nodes).filteredOn(n -> n.id() == b).first().satisfies(n -> {
            assertThat(n.depth()).isEqualTo(1);
            assertThat(n.descendantDocumentCount()).isEqualTo(1);
        });
        assertThat(nodes).filteredOn(n -> n.id() == d).first().satisfies(n -> {
            assertThat(n.depth()).isEqualTo(3);
            assertThat(n.descendantDocumentCount()).isZero();
        });
    }

    @Test
    void existingCategoriesBecomeRootsAndUserIsolationHolds() {
        // 未指定 parentId 的分类都是根
        long a = service.createCategory(new CreateKnowledgeCategoryRequest("A")).response().id();
        long b = service.createCategory(new CreateKnowledgeCategoryRequest("B", a)).response().id();
        // user2 建自己的根（同名允许：同用户全局唯一，跨用户独立）
        long otherRoot = repository.insertCategory(2L, "B", "b");

        List<KnowledgeCategoryNodeResponse> mine = service.listCategoryTree();
        assertThat(mine).filteredOn(n -> n.id() == b).first().satisfies(n -> assertThat(n.parentId()).isEqualTo(a));
        // user2 的分类不出现在 user1 列表
        assertThat(mine).noneMatch(n -> n.id() == otherRoot);
        // 跨用户 parent 拒绝
        assertThatThrownBy(() -> service.createCategory(new CreateKnowledgeCategoryRequest("C", otherRoot)))
                .isInstanceOf(java.util.NoSuchElementException.class);
    }

    @Test
    void rejectsSelfAndDescendantCyclesAndBeyondFiveLevels() {
        long a = service.createCategory(new CreateKnowledgeCategoryRequest("A")).response().id();
        long b = service.createCategory(new CreateKnowledgeCategoryRequest("B", a)).response().id();
        long c = service.createCategory(new CreateKnowledgeCategoryRequest("C", b)).response().id();

        // 自身为父
        assertThatThrownBy(() -> service.updateCategory(b, new UpdateKnowledgeCategoryRequest("B", b)))
                .isInstanceOf(IllegalArgumentException.class);
        // 后代为父
        assertThatThrownBy(() -> service.updateCategory(a, new UpdateKnowledgeCategoryRequest("A", c)))
                .isInstanceOf(IllegalArgumentException.class);
        // 超过五级
        long d = service.createCategory(new CreateKnowledgeCategoryRequest("D", c)).response().id();
        long e = service.createCategory(new CreateKnowledgeCategoryRequest("E", d)).response().id();
        assertThatThrownBy(() -> service.createCategory(new CreateKnowledgeCategoryRequest("F", e)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("五级");
        // 失败后深度未变：e 仍在 depth 4
        assertThat(service.listCategoryTree()).filteredOn(n -> n.id() == e).first()
                .satisfies(n -> assertThat(n.depth()).isEqualTo(4));
    }

    @Test
    void deleteOnlyEmptyLeafAndFailedDeleteChangesNothing() {
        long a = service.createCategory(new CreateKnowledgeCategoryRequest("A")).response().id();
        long b = service.createCategory(new CreateKnowledgeCategoryRequest("B", a)).response().id();
        repository.setDocumentCategory(1L, docId(1), a);

        // a 有子分类 → 拒绝
        assertThatThrownBy(() -> service.deleteCategory(a))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("CATEGORY_NOT_EMPTY");
        // b 空叶 → 删除成功
        service.deleteCategory(b);
        assertThat(repository.findCategoryById(1L, b)).isEmpty();
        // a 仍有直属文档 → 拒绝
        assertThatThrownBy(() -> service.deleteCategory(a))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("CATEGORY_NOT_EMPTY");
        // 数据未改变
        assertThat(repository.findCategoryById(1L, a)).isPresent();
        assertThat(repository.findDocumentCategory(1L, docId(1))).isPresent();
    }

    @Test
    void includeDescendantsSearchIncludesOffspringAndStacksWithTags() {
        long a = service.createCategory(new CreateKnowledgeCategoryRequest("A")).response().id();
        long b = service.createCategory(new CreateKnowledgeCategoryRequest("B", a)).response().id();
        repository.setDocumentCategory(1L, docId(1), a);
        repository.setDocumentCategory(1L, docId(2), b);
        long tag = repository.insertTag(1L, "标签", "标签");
        repository.addDocumentTag(1L, docId(2), tag);

        // false 只返回直属
        List<KnowledgeSearchItemResponse> direct = service.search("文档", a, null, false);
        assertThat(direct).extracting(r -> r.document().id()).containsExactly(docId(1));
        // true 包含后代
        List<KnowledgeSearchItemResponse> descendants = service.search("文档", a, null, true);
        assertThat(descendants).extracting(r -> r.document().id()).containsExactlyInAnyOrder(docId(1), docId(2));
        // 标签筛选叠加
        List<KnowledgeSearchItemResponse> withTag = service.search("文档", a, tag, true);
        assertThat(withTag).extracting(r -> r.document().id()).containsExactly(docId(2));
    }

    @Test
    void moveKeepsCountsAndSearchConsistent() {
        long a = service.createCategory(new CreateKnowledgeCategoryRequest("A")).response().id();
        long b = service.createCategory(new CreateKnowledgeCategoryRequest("B")).response().id();
        long c = service.createCategory(new CreateKnowledgeCategoryRequest("C", b)).response().id();
        repository.setDocumentCategory(1L, docId(1), a);
        repository.setDocumentCategory(1L, docId(2), c);

        // 把 C 移到 A 下
        service.updateCategory(c, new UpdateKnowledgeCategoryRequest("C", a));

        List<KnowledgeCategoryNodeResponse> nodes = service.listCategoryTree();
        assertThat(nodes).filteredOn(n -> n.id() == c).first().satisfies(n -> {
            assertThat(n.parentId()).isEqualTo(a);
            assertThat(n.depth()).isEqualTo(1);
        });
        assertThat(nodes).filteredOn(n -> n.id() == b).first().satisfies(n -> {
            assertThat(n.descendantDocumentCount()).isZero();
        });
        assertThat(nodes).filteredOn(n -> n.id() == a).first()
                .satisfies(n -> assertThat(n.descendantDocumentCount()).isEqualTo(2));
        // 搜索一致
        assertThat(service.search("文档", a, null, true))
                .extracting(r -> r.document().id()).containsExactlyInAnyOrder(docId(1), docId(2));
    }
}
