package com.resumego.knowledge;

import com.resumego.knowledge.dto.CreateKnowledgeNameRequest;
import com.resumego.knowledge.dto.KnowledgeCategoryResponse;
import com.resumego.knowledge.dto.KnowledgeSearchItemResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

class KnowledgeClassificationServiceTest {

    private KnowledgeRepository repository;
    private KnowledgeClassificationService service;

    @BeforeEach
    void setUp() {
        repository = mock(KnowledgeRepository.class);
        service = new KnowledgeClassificationService(repository);
    }

    private KnowledgeCategory category(long id, String name, String normalized) {
        return new KnowledgeCategory(id, 1L, name, normalized, LocalDateTime.now(), LocalDateTime.now());
    }

    private KnowledgeTag tag(long id, String name, String normalized) {
        return new KnowledgeTag(id, 1L, name, normalized, LocalDateTime.now(), LocalDateTime.now());
    }

    private KnowledgeDocument doc(long id) {
        return new KnowledgeDocument(id, 1L, "标题", "NOTE", "NOT_STARTED",
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void normalizesNameCollapsingInternalWhitespace() {
        when(repository.findCategoryByNormalizedName(1L, "求职 准备")).thenReturn(Optional.empty());
        when(repository.insertCategory(1L, "求职 准备", "求职 准备")).thenReturn(12L);
        when(repository.findCategoryById(1L, 12L)).thenReturn(Optional.of(category(12L, "求职 准备", "求职 准备")));

        KnowledgeNameCreateResult<KnowledgeCategoryResponse> result =
                service.createCategory(new CreateKnowledgeNameRequest("  求职  准备  "));

        assertThat(result.created()).isTrue();
        assertThat(result.response().name()).isEqualTo("求职 准备");
        assertThat(result.response().normalizedName()).isEqualTo("求职 准备");
    }

    @Test
    void rejectsBlankName() {
        assertThatThrownBy(() -> service.createCategory(new CreateKnowledgeNameRequest("   ")))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("名称");
        assertThatThrownBy(() -> service.createTag(new CreateKnowledgeNameRequest(null)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(repository, never()).insertCategory(anyLong(), any(), any());
    }

    @Test
    void rejectsNameLongerThan40() {
        assertThatThrownBy(() -> service.createCategory(new CreateKnowledgeNameRequest("a".repeat(41))))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("40");
    }

    @Test
    void returnsExistingForDuplicateNormalizedName() {
        when(repository.findCategoryByNormalizedName(1L, "求职")).thenReturn(Optional.of(category(5L, "求职", "求职")));

        KnowledgeNameCreateResult<KnowledgeCategoryResponse> result =
                service.createCategory(new CreateKnowledgeNameRequest("  求职  "));

        assertThat(result.created()).isFalse();
        assertThat(result.response().id()).isEqualTo(5L);
        verify(repository, never()).insertCategory(anyLong(), any(), any());
    }

    @Test
    void normalizesEnglishNamesToLowerCaseRoot() {
        when(repository.findTagByNormalizedName(1L, "tensorflow")).thenReturn(Optional.empty());
        when(repository.insertTag(1L, "TensorFlow", "tensorflow")).thenReturn(7L);
        when(repository.findTagById(1L, 7L)).thenReturn(Optional.of(tag(7L, "TensorFlow", "tensorflow")));

        service.createTag(new CreateKnowledgeNameRequest("TensorFlow"));

        verify(repository).insertTag(1L, "TensorFlow", "tensorflow");
    }

    @Test
    void relationRequiresOwnedDocumentAndCategory() {
        when(repository.findById(1L, 404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.setDocumentCategory(404L, 1L))
                .isInstanceOf(NoSuchElementException.class);

        when(repository.findById(1L, 10L)).thenReturn(Optional.of(doc(10L)));
        when(repository.findCategoryById(1L, 404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.setDocumentCategory(10L, 404L))
                .isInstanceOf(NoSuchElementException.class);
        // 外部用户的分类对当前用户不可见 -> 404
        when(repository.findCategoryById(1L, 3L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.setDocumentCategory(10L, 3L))
                .isInstanceOf(NoSuchElementException.class);
        verify(repository, never()).setDocumentCategory(anyLong(), anyLong(), anyLong());
    }

    @Test
    void tagRelationRequiresOwnedDocumentAndTag() {
        when(repository.findById(1L, 404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.addDocumentTag(404L, 1L))
                .isInstanceOf(NoSuchElementException.class);

        when(repository.findById(1L, 10L)).thenReturn(Optional.of(doc(10L)));
        when(repository.findTagById(1L, 404L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.removeDocumentTag(10L, 404L))
                .isInstanceOf(NoSuchElementException.class);
        verify(repository, never()).addDocumentTag(anyLong(), anyLong(), anyLong());
    }

    @Test
    void searchRejectsInvalidQueryAndForeignFilters() {
        assertThatThrownBy(() -> service.search("   ", null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.search("a".repeat(101), null, null))
                .isInstanceOf(IllegalArgumentException.class);

        when(repository.findCategoryById(1L, 99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.search("笔记", 99L, null))
                .isInstanceOf(NoSuchElementException.class);

        when(repository.findTagById(1L, 98L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.search("笔记", null, 98L))
                .isInstanceOf(NoSuchElementException.class);
        verify(repository, never()).search(anyLong(), any(), any(), any());
    }

    @Test
    void contentHitProducesSnippetAndOneBasedLineNumber() {
        String content = "第一行\nTensorFlow 学习笔记\n第三行";
        when(repository.search(eq(1L), any(), isNull(), isNull())).thenReturn(List.of(
                new KnowledgeSearchRow(1L, "标题", "FILE", "COMPLETED", "t", "t", "CONTENT", content)));

        List<KnowledgeSearchItemResponse> results = service.search("TensorFlow", null, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).matchedField()).isEqualTo("CONTENT");
        assertThat(results.get(0).snippet()).contains("TensorFlow");
        assertThat(results.get(0).lineNumber()).isEqualTo(2);
        assertThat(results.get(0).document().title()).isEqualTo("标题");
    }

    @Test
    void titleHitHasNullLineNumberAndNoFullText() {
        when(repository.search(eq(1L), any(), isNull(), isNull())).thenReturn(List.of(
                new KnowledgeSearchRow(1L, "TensorFlow 学习笔记", "NOTE", "NOT_STARTED", "t", "t", "TITLE", null)));

        List<KnowledgeSearchItemResponse> results = service.search("tensorflow", null, null);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).matchedField()).isEqualTo("TITLE");
        assertThat(results.get(0).lineNumber()).isNull();
        assertThat(results.get(0).snippet()).contains("TensorFlow");
    }

    @Test
    void escapeLikeEscapesWildcardsLiterally() {
        assertThat(KnowledgeClassificationService.escapeLike("100%_x\\y"))
                .isEqualTo("100\\%\\_x\\\\y");
    }

    @Test
    void snippetStaysWithinLimitWithEllipsesAndLineNumberAtStartIsOne() {
        String longContent = "a".repeat(500) + "关键词" + "b".repeat(500);
        when(repository.search(eq(1L), any(), isNull(), isNull())).thenReturn(List.of(
                new KnowledgeSearchRow(1L, "标题", "FILE", "COMPLETED", "t", "t", "CONTENT", longContent)));

        List<KnowledgeSearchItemResponse> results = service.search("关键词", null, null);

        assertThat(results.get(0).snippet()).contains("关键词");
        assertThat(results.get(0).snippet()).hasSizeLessThanOrEqualTo(240);
        assertThat(results.get(0).snippet()).startsWith("…");
        assertThat(results.get(0).lineNumber()).isEqualTo(1);
    }
}
