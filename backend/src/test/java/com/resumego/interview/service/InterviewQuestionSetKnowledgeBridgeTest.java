package com.resumego.interview.service;

import com.resumego.interview.QuestionSourceType;
import com.resumego.interview.repository.InterviewQuestionSetRepository;
import com.resumego.knowledge.KnowledgeClassificationService;
import com.resumego.knowledge.KnowledgeService;
import com.resumego.knowledge.dto.KnowledgeContentResponse;
import com.resumego.knowledge.dto.KnowledgeDocumentResponse;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class InterviewQuestionSetKnowledgeBridgeTest {

    @Test
    void previewsKnowledgeExperienceFormatWithoutCreatingAQuestionSet() {
        InterviewQuestionSetRepository repository = mock(InterviewQuestionSetRepository.class);
        KnowledgeService knowledge = mock(KnowledgeService.class);
        KnowledgeClassificationService classification = mock(KnowledgeClassificationService.class);
        InterviewQuestionSetService service = new InterviewQuestionSetService(repository, knowledge, classification);

        when(classification.isDocumentUnderCategoryNamed(9L, "真实面经")).thenReturn(true);
        when(knowledge.get(9L)).thenReturn(new KnowledgeDocumentResponse(
                9L, "腾讯技术面经", "NOTE", "COMPLETED", null, null, null,
                "2026-08-27T09:00:00", "2026-08-27T10:00:00"));
        when(knowledge.getContent(9L)).thenReturn(new KnowledgeContentResponse(9L,
                "---\ncompany: 腾讯\nrole: Java 后端\nicon: tencent\n---\n1. 讲讲 Redis\n2. 如何排查慢查询？"));

        var result = service.previewKnowledgeDocument(9L);

        assertThat(result.status()).isEqualTo("READY");
        assertThat(result.questionCount()).isEqualTo(2);
        assertThat(result.companyName()).isEqualTo("腾讯");
        assertThat(result.targetRole()).isEqualTo("Java 后端");
        verify(repository, never()).createSet(anyLong(), anyString(), any(), anyString(), anyList());
    }

    @Test
    void reportsInvalidKnowledgeExperienceFormatWithoutInventingQuestions() {
        InterviewQuestionSetRepository repository = mock(InterviewQuestionSetRepository.class);
        KnowledgeService knowledge = mock(KnowledgeService.class);
        KnowledgeClassificationService classification = mock(KnowledgeClassificationService.class);
        InterviewQuestionSetService service = new InterviewQuestionSetService(repository, knowledge, classification);

        when(classification.isDocumentUnderCategoryNamed(9L, "真实面经")).thenReturn(true);
        when(knowledge.get(9L)).thenReturn(new KnowledgeDocumentResponse(
                9L, "未整理面经", "NOTE", "COMPLETED", null, null, null,
                "2026-08-27T09:00:00", "2026-08-27T10:00:00"));
        when(knowledge.getContent(9L)).thenReturn(new KnowledgeContentResponse(9L, "只有一段没有编号的问题描述"));

        var result = service.previewKnowledgeDocument(9L);

        assertThat(result.status()).isEqualTo("INVALID");
        assertThat(result.questionCount()).isZero();
        assertThat(result.message()).contains("序号或列表");
        verify(repository, never()).createSet(anyLong(), anyString(), any(), anyString(), anyList());
    }

    @Test
    void materializesOnlyDocumentsFromTheRealExperienceFolder() {
        InterviewQuestionSetRepository repository = mock(InterviewQuestionSetRepository.class);
        KnowledgeService knowledge = mock(KnowledgeService.class);
        KnowledgeClassificationService classification = mock(KnowledgeClassificationService.class);
        InterviewQuestionSetService service = new InterviewQuestionSetService(repository, knowledge, classification);

        when(classification.isDocumentUnderCategoryNamed(9L, "真实面经")).thenReturn(true);
        when(repository.findSetIdBySourceDocument(1L, 9L)).thenReturn(Optional.empty());
        when(knowledge.get(9L)).thenReturn(new KnowledgeDocumentResponse(
                9L, "腾讯技术面经", "NOTE", "COMPLETED", null, null, null,
                "2026-08-27T09:00:00", "2026-08-27T10:00:00"));
        when(knowledge.getContent(9L)).thenReturn(new KnowledgeContentResponse(9L,
                "---\ncompany: 腾讯\nrole: Java 后端\nicon: tencent\n---\n1. 讲讲 Redis\n2. 如何排查慢查询？"));
        when(repository.createSet(eq(1L), eq("腾讯技术面经"), eq(QuestionSourceType.USER_MANUAL),
                contains("知识库"), eq("腾讯"), eq("Java 后端"), eq("tencent"), eq(9L),
                eq(List.of("讲讲 Redis", "如何排查慢查询？")))).thenReturn(4L);
        when(repository.findSetById(1L, 4L)).thenReturn(new InterviewQuestionSetRepository.QuestionSetRow(
                4L, "腾讯技术面经", QuestionSourceType.USER_MANUAL, "知识库资料：腾讯技术面经",
                "腾讯", "Java 后端", "tencent", 9L, 2, false, null,
                LocalDateTime.now(), LocalDateTime.now()));
        when(repository.findQuestionTexts(4L)).thenReturn(List.of("讲讲 Redis", "如何排查慢查询？"));

        var result = service.createFromKnowledgeDocument(9L);

        assertThat(result.id()).isEqualTo(4L);
        assertThat(result.sourceDocumentId()).isEqualTo(9L);
        assertThat(result.items()).extracting(item -> item.questionText())
                .containsExactly("讲讲 Redis", "如何排查慢查询？");
        verify(repository).createSet(eq(1L), eq("腾讯技术面经"), eq(QuestionSourceType.USER_MANUAL),
                anyString(), eq("腾讯"), eq("Java 后端"), eq("tencent"), eq(9L), anyList());
    }

    @Test
    void refreshesExistingQuestionSetFromLatestKnowledgeContent() {
        InterviewQuestionSetRepository repository = mock(InterviewQuestionSetRepository.class);
        KnowledgeService knowledge = mock(KnowledgeService.class);
        KnowledgeClassificationService classification = mock(KnowledgeClassificationService.class);
        InterviewQuestionSetService service = new InterviewQuestionSetService(repository, knowledge, classification);

        when(classification.isDocumentUnderCategoryNamed(9L, "真实面经")).thenReturn(true);
        when(repository.findSetIdBySourceDocument(1L, 9L)).thenReturn(Optional.of(4L));
        when(knowledge.get(9L)).thenReturn(new KnowledgeDocumentResponse(
                9L, "字节技术面经", "NOTE", "COMPLETED", null, null, null,
                "2026-08-27T09:00:00", "2026-08-27T11:00:00"));
        when(knowledge.getContent(9L)).thenReturn(new KnowledgeContentResponse(9L,
                "---\ncompany: 字节跳动\nrole: Java 后端\nicon: bytedance\n---\n"
                        + "1. 讲讲 Redis 缓存一致性\n2. 如何排查慢查询？\n3. 介绍一次故障排查"));
        when(repository.findSetById(1L, 4L)).thenReturn(new InterviewQuestionSetRepository.QuestionSetRow(
                4L, "字节技术面经", QuestionSourceType.USER_MANUAL, "知识库资料：字节技术面经",
                "字节跳动", "Java 后端", "bytedance", 9L, 3, false, null,
                LocalDateTime.now(), LocalDateTime.now()));
        when(repository.findQuestionTexts(4L)).thenReturn(List.of(
                "讲讲 Redis 缓存一致性", "如何排查慢查询？", "介绍一次故障排查"));

        var result = service.createFromKnowledgeDocument(9L);

        assertThat(result.id()).isEqualTo(4L);
        assertThat(result.companyName()).isEqualTo("字节跳动");
        assertThat(result.sourceDocumentId()).isEqualTo(9L);
        assertThat(result.items()).extracting(item -> item.questionText())
                .containsExactly("讲讲 Redis 缓存一致性", "如何排查慢查询？", "介绍一次故障排查");
        verify(repository).replaceSet(eq(1L), eq(4L), eq("字节技术面经"), eq(QuestionSourceType.USER_MANUAL),
                contains("知识库"), eq("字节跳动"), eq("Java 后端"), eq("bytedance"),
                eq(List.of("讲讲 Redis 缓存一致性", "如何排查慢查询？", "介绍一次故障排查")));
        verify(repository, never()).createSet(anyLong(), anyString(), any(), anyString(), anyList());
    }

    @Test
    void refreshesLinkedQuestionSetWhenListingAfterKnowledgeDocumentChanges() {
        InterviewQuestionSetRepository repository = mock(InterviewQuestionSetRepository.class);
        KnowledgeService knowledge = mock(KnowledgeService.class);
        KnowledgeClassificationService classification = mock(KnowledgeClassificationService.class);
        InterviewQuestionSetService service = new InterviewQuestionSetService(repository, knowledge, classification);

        LocalDateTime previousUpdate = LocalDateTime.of(2026, 8, 26, 18, 0);
        when(repository.findAllSets(1L)).thenReturn(List.of(new InterviewQuestionSetRepository.QuestionSetRow(
                4L, "腾讯技术面经", QuestionSourceType.USER_MANUAL, "知识库资料：腾讯技术面经",
                "腾讯", "Java 后端", "tencent", 9L, 2, false, null,
                previousUpdate.minusDays(1), previousUpdate)));
        when(classification.isDocumentUnderCategoryNamed(9L, "真实面经")).thenReturn(true);
        when(knowledge.get(9L)).thenReturn(new KnowledgeDocumentResponse(
                9L, "腾讯技术面经", "NOTE", "COMPLETED", null, null, null,
                "2026-08-27T09:00:00", "2026-08-27T11:00:00"));
        when(knowledge.getContent(9L)).thenReturn(new KnowledgeContentResponse(9L,
                "---\ncompany: 腾讯\nrole: Java 后端\nicon: tencent\n---\n"
                        + "1. 讲讲 Redis 缓存一致性\n2. 如何排查慢查询？\n3. 介绍一次故障排查"));
        when(repository.findSetById(1L, 4L)).thenReturn(new InterviewQuestionSetRepository.QuestionSetRow(
                4L, "腾讯技术面经", QuestionSourceType.USER_MANUAL, "知识库资料：腾讯技术面经",
                "腾讯", "Java 后端", "tencent", 9L, 3, false, null,
                previousUpdate.minusDays(1), LocalDateTime.of(2026, 8, 27, 11, 0)));

        var result = service.list();

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.questionCount()).isEqualTo(3);
            assertThat(item.companyName()).isEqualTo("腾讯");
            assertThat(item.companyIconKey()).isEqualTo("tencent");
        });
        verify(repository).replaceSet(eq(1L), eq(4L), eq("腾讯技术面经"), eq(QuestionSourceType.USER_MANUAL),
                contains("知识库"), eq("腾讯"), eq("Java 后端"), eq("tencent"),
                eq(List.of("讲讲 Redis 缓存一致性", "如何排查慢查询？", "介绍一次故障排查")));
    }

    @Test
    void derivesCurrentCompanyIconWhenCompanyChangedButOldIconWasNotEdited() {
        InterviewQuestionSetRepository repository = mock(InterviewQuestionSetRepository.class);
        KnowledgeService knowledge = mock(KnowledgeService.class);
        KnowledgeClassificationService classification = mock(KnowledgeClassificationService.class);
        InterviewQuestionSetService service = new InterviewQuestionSetService(repository, knowledge, classification);

        when(repository.findAllSets(1L)).thenReturn(List.of(new InterviewQuestionSetRepository.QuestionSetRow(
                4L, "公司面经", QuestionSourceType.USER_MANUAL, "知识库资料：公司面经",
                "腾讯", "Java 后端", "tencent", 9L, 1, false, null,
                LocalDateTime.of(2026, 8, 26, 18, 0), LocalDateTime.of(2026, 8, 26, 18, 0))));
        when(classification.isDocumentUnderCategoryNamed(9L, "真实面经")).thenReturn(true);
        when(knowledge.get(9L)).thenReturn(new KnowledgeDocumentResponse(
                9L, "公司面经", "NOTE", "COMPLETED", null, null, null,
                "2026-08-27T09:00:00", "2026-08-27T11:00:00"));
        when(knowledge.getContent(9L)).thenReturn(new KnowledgeContentResponse(9L,
                "---\ncompany: 字节跳动\nrole: Java 后端\nicon: tencent\n---\n1. 讲讲缓存"));
        when(repository.findSetById(1L, 4L)).thenReturn(new InterviewQuestionSetRepository.QuestionSetRow(
                4L, "公司面经", QuestionSourceType.USER_MANUAL, "知识库资料：公司面经",
                "字节跳动", "Java 后端", null, 9L, 1, false, null,
                LocalDateTime.of(2026, 8, 26, 18, 0), LocalDateTime.of(2026, 8, 27, 11, 0)));

        var result = service.list();

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.companyName()).isEqualTo("字节跳动");
            assertThat(item.companyIconKey()).isNull();
        });
        verify(repository).replaceSet(eq(1L), eq(4L), eq("公司面经"), eq(QuestionSourceType.USER_MANUAL),
                contains("知识库"), eq("字节跳动"), eq("Java 后端"), isNull(), eq(List.of("讲讲缓存")));
    }
}
