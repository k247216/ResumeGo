package com.resumego.knowledge;

import com.resumego.knowledge.dto.CreateKnowledgeCategoryRequest;
import com.resumego.knowledge.dto.KnowledgeCategoryNodeResponse;
import com.resumego.knowledge.dto.UpdateKnowledgeCategoryRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class KnowledgeHierarchyServiceTest {

    private KnowledgeRepository repository;
    private KnowledgeClassificationService service;

    @BeforeEach
    void setUp() {
        repository = mock(KnowledgeRepository.class);
        service = new KnowledgeClassificationService(repository);
    }

    private KnowledgeCategory category(long id, String name, Long parentId) {
        return new KnowledgeCategory(id, 1L, name, name, parentId, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void listCategoryTreeComputesDepthAndDocumentCounts() {
        // root A(1) -> B(2) -> C(3)；A 直属 1 篇，B 直属 2 篇，C 直属 3 篇
        when(repository.listCategories(1L)).thenReturn(List.of(
                category(1, "A", null), category(2, "B", 1L), category(3, "C", 2L)));
        when(repository.listCategoryDocumentCounts(1L)).thenReturn(Map.of(1L, 1, 2L, 2, 3L, 3));

        List<KnowledgeCategoryNodeResponse> nodes = service.listCategoryTree();

        assertThat(nodes).hasSize(3);
        KnowledgeCategoryNodeResponse a = nodes.stream().filter(n -> n.id() == 1).findFirst().orElseThrow();
        KnowledgeCategoryNodeResponse b = nodes.stream().filter(n -> n.id() == 2).findFirst().orElseThrow();
        KnowledgeCategoryNodeResponse c = nodes.stream().filter(n -> n.id() == 3).findFirst().orElseThrow();
        assertThat(a.depth()).isZero();
        assertThat(a.directDocumentCount()).isEqualTo(1);
        assertThat(a.descendantDocumentCount()).isEqualTo(6); // 1 + 2 + 3
        assertThat(b.depth()).isEqualTo(1);
        assertThat(b.descendantDocumentCount()).isEqualTo(5); // 2 + 3
        assertThat(c.depth()).isEqualTo(2);
        assertThat(c.descendantDocumentCount()).isEqualTo(3);
    }

    @Test
    void createCategoryWithParentChecksOwnershipAndDepth() {
        when(repository.findCategoryById(1L, 1L)).thenReturn(Optional.of(category(1, "根", null)));
        when(repository.listCategories(1L)).thenReturn(List.of(category(1, "根", null)));
        when(repository.findCategoryByNormalizedName(1L, "新分类")).thenReturn(Optional.empty());
        when(repository.insertCategoryWithParent(1L, "新分类", "新分类", 1L)).thenReturn(10L);
        when(repository.findCategoryById(1L, 10L)).thenReturn(Optional.of(category(10, "新分类", 1L)));

        var result = service.createCategory(new CreateKnowledgeCategoryRequest("新分类", 1L));
        assertThat(result.created()).isTrue();
        verify(repository).insertCategoryWithParent(1L, "新分类", "新分类", 1L);

        // 跨用户 parent
        when(repository.findCategoryById(1L, 404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.createCategory(new CreateKnowledgeCategoryRequest("x", 404L)))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void createCategoryRejectsDepthBeyondFiveLevels() {
        // 构造五级链 1->2->3->4->5（depth 4）；在其下创建 depth 5 被拒
        when(repository.findCategoryById(1L, 5L)).thenReturn(Optional.of(category(5, "L5", 4L)));
        when(repository.listCategories(1L)).thenReturn(List.of(
                category(1, "L1", null), category(2, "L2", 1L), category(3, "L3", 2L),
                category(4, "L4", 3L), category(5, "L5", 4L)));
        assertThatThrownBy(() -> service.createCategory(new CreateKnowledgeCategoryRequest("L6", 5L)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("五级");
        verify(repository, never()).insertCategoryWithParent(anyLong(), any(), any(), any());
    }

    @Test
    void updateCategoryRejectsSelfParentAndDescendantCycle() {
        when(repository.findCategoryById(1L, 2L)).thenReturn(Optional.of(category(2, "B", 1L)));
        when(repository.findCategoryById(1L, 3L)).thenReturn(Optional.of(category(3, "C", 2L)));
        when(repository.listCategories(1L)).thenReturn(List.of(
                category(1, "A", null), category(2, "B", 1L), category(3, "C", 2L)));

        // 自身为父
        assertThatThrownBy(() -> service.updateCategory(2L, new UpdateKnowledgeCategoryRequest("B", 2L)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("自身");
        // 后代为父（C 是 B 的后代）
        when(repository.findCategoryById(1L, 3L)).thenReturn(Optional.of(category(3, "C", 2L)));
        assertThatThrownBy(() -> service.updateCategory(2L, new UpdateKnowledgeCategoryRequest("B", 3L)))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("后代");
        verify(repository, never()).updateCategory(anyLong(), anyLong(), any(), any(), any());
    }

    @Test
    void deleteCategoryRejectsNonEmptyLeafWithoutChangingData() {
        when(repository.findCategoryById(1L, 1L)).thenReturn(Optional.of(category(1, "A", null)));
        when(repository.listCategories(1L)).thenReturn(List.of(category(1, "A", null), category(2, "B", 1L)));
        when(repository.listCategoryDocumentCounts(1L)).thenReturn(Map.of());
        assertThatThrownBy(() -> service.deleteCategory(1L))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("CATEGORY_NOT_EMPTY");
        verify(repository, never()).deleteCategoryById(anyLong(), anyLong());

        // 有直属文档也拒绝
        when(repository.findCategoryById(1L, 3L)).thenReturn(Optional.of(category(3, "C", null)));
        when(repository.listCategories(1L)).thenReturn(List.of(category(3, "C", null)));
        when(repository.listCategoryDocumentCounts(1L)).thenReturn(Map.of(3L, 2));
        assertThatThrownBy(() -> service.deleteCategory(3L))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("CATEGORY_NOT_EMPTY");

        // 空叶成功
        when(repository.listCategoryDocumentCounts(1L)).thenReturn(Map.of());
        service.deleteCategory(3L);
        verify(repository).deleteCategoryById(1L, 3L);
    }

    @Test
    void searchWithIncludeDescendantsPassesSubtreeIds() {
        when(repository.listCategories(1L)).thenReturn(List.of(
                category(1, "A", null), category(2, "B", 1L), category(3, "C", 1L)));
        when(repository.search(eq(1L), any(), eq(java.util.Set.of(1L, 2L, 3L)), isNull()))
                .thenReturn(List.of());
        when(repository.findCategoryById(1L, 1L)).thenReturn(Optional.of(category(1, "A", null)));

        service.search("x", 1L, null, true);

        verify(repository).search(eq(1L), any(), eq(java.util.Set.of(1L, 2L, 3L)), isNull());
    }
}
