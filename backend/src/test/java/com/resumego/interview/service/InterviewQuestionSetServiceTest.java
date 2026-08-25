package com.resumego.interview.service;

import com.resumego.common.CurrentUser;
import com.resumego.interview.QuestionSourceType;
import com.resumego.interview.dto.InterviewQuestionSetRequest;
import com.resumego.interview.dto.InterviewQuestionSetResponse;
import com.resumego.interview.repository.InterviewQuestionSetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 面经题集服务测试：校验、越权、归档与回滚语义。 */
class InterviewQuestionSetServiceTest {

    private FakeQuestionSetRepository repository;
    private InterviewQuestionSetService service;

    @BeforeEach
    void setUp() {
        repository = new FakeQuestionSetRepository();
        service = new InterviewQuestionSetService(repository);
    }

    private InterviewQuestionSetRequest request(String title, QuestionSourceType type, String note, List<String> questions) {
        return new InterviewQuestionSetRequest(title, type, note, questions);
    }

    @Test
    @DisplayName("创建题集：返回有序题目详情")
    void shouldCreateSetWithOrderedQuestions() {
        InterviewQuestionSetResponse response = service.create(request(
                "腾讯面经", QuestionSourceType.IMPORTED_EXPERIENCE, "牛客 2026",
                List.of("讲讲 JVM", "Redis 持久化")));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("腾讯面经");
        assertThat(response.items()).extracting(InterviewQuestionSetResponse.QuestionItem::positionIndex)
                .containsExactly(0, 1);
        assertThat(response.items()).extracting(InterviewQuestionSetResponse.QuestionItem::questionText)
                .containsExactly("讲讲 JVM", "Redis 持久化");
    }

    @Test
    @DisplayName("空题集、空白题目、超长题目、超长来源说明均被拒绝且不写库")
    void shouldRejectInvalidRequests() {
        assertThatThrownBy(() -> service.create(request("x", QuestionSourceType.USER_MANUAL, null, List.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("至少包含一道题目");

        assertThatThrownBy(() -> service.create(request("x", QuestionSourceType.USER_MANUAL, null, List.of("  "))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不能为空");

        StringBuilder longQuestion = new StringBuilder();
        for (int i = 0; i < 1001; i++) longQuestion.append('题');
        assertThatThrownBy(() -> service.create(request("x", QuestionSourceType.USER_MANUAL, null, List.of(longQuestion.toString()))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1000");

        assertThatThrownBy(() -> service.create(request("x", QuestionSourceType.USER_MANUAL, "长".repeat(501), List.of("q"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("来源说明");

        assertThatThrownBy(() -> service.create(request(null, QuestionSourceType.USER_MANUAL, null, List.of("q"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("题集名称");

        assertThatThrownBy(() -> service.create(request("x", null, null, List.of("q"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("来源类型");

        assertThat(repository.createCalls).isZero();
    }

    @Test
    @DisplayName("所有者可读取详情；列表不含题目正文；不存在的题集按不存在处理")
    void shouldScopeReadsToCurrentUser() {
        service.create(request("我的题集", QuestionSourceType.USER_MANUAL, null, List.of("题目一")));

        InterviewQuestionSetResponse detail = service.get(1L);
        assertThat(detail.items()).extracting(InterviewQuestionSetResponse.QuestionItem::questionText)
                .containsExactly("题目一");

        assertThatThrownBy(() -> service.get(999L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("面经题集不存在");

        List<InterviewQuestionSetResponse> rows = service.list();
        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).items()).isNull();
    }

    @Test
    @DisplayName("更新原子替换元数据与题目")
    void shouldReplaceSetAtomically() {
        service.create(request("旧标题", QuestionSourceType.USER_MANUAL, null, List.of("旧题")));

        InterviewQuestionSetResponse updated = service.update(1L, request(
                "新标题", QuestionSourceType.IMPORTED_EXPERIENCE, "来源", List.of("新题一", "新题二")));

        assertThat(updated.title()).isEqualTo("新标题");
        assertThat(updated.sourceType()).isEqualTo(QuestionSourceType.IMPORTED_EXPERIENCE);
        assertThat(updated.items()).extracting(InterviewQuestionSetResponse.QuestionItem::questionText)
                .containsExactly("新题一", "新题二");
        assertThat(repository.replacedSetId).isEqualTo(1L);
    }

    @Test
    @DisplayName("归档后不可修改，重复归档无副作用；跨用户归档 404")
    void shouldArchiveWithoutSideEffects() {
        service.create(request("题集", QuestionSourceType.USER_MANUAL, null, List.of("题")));

        service.archive(1L);
        LocalDateTime firstArchiveAt = repository.archivedAtBySet.get(1L);
        assertThat(firstArchiveAt).isNotNull();

        service.archive(1L);
        assertThat(repository.archivedAtBySet.get(1L)).isEqualTo(firstArchiveAt);

        assertThatThrownBy(() -> service.update(1L, request("改", QuestionSourceType.USER_MANUAL, null, List.of("题"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("已归档");

        assertThatThrownBy(() -> service.archive(999L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("面经题集不存在");
    }

    private static class FakeQuestionSetRepository extends InterviewQuestionSetRepository {
        final java.util.Map<Long, QuestionSetRow> rows = new java.util.LinkedHashMap<>();
        final java.util.Map<Long, List<String>> items = new java.util.LinkedHashMap<>();
        final java.util.Map<Long, LocalDateTime> archivedAtBySet = new java.util.HashMap<>();
        int createCalls;
        Long replacedSetId;
        long nextId = 1;

        FakeQuestionSetRepository() {
            super(null);
        }

        private QuestionSetRow row(long id) {
            QuestionSetRow base = rows.get(id);
            LocalDateTime archivedAt = archivedAtBySet.get(id);
            return new QuestionSetRow(base.id(), base.title(), base.sourceType(), base.sourceNote(),
                    archivedAt != null, archivedAt, base.createdAt(), base.updatedAt());
        }

        @Override
        public long createSet(long userId, String title, QuestionSourceType sourceType,
                              String sourceNote, List<String> questions) {
            createCalls++;
            long id = nextId++;
            rows.put(id, new QuestionSetRow(id, title, sourceType, sourceNote, false,
                    null, LocalDateTime.now(), LocalDateTime.now()));
            items.put(id, new java.util.ArrayList<>(questions));
            return id;
        }

        @Override
        public QuestionSetRow findSetById(long userId, long setId) {
            // 只有用户 1 的数据存在；其他用户读不到
            if (userId != CurrentUser.DEMO_USER_ID || !rows.containsKey(setId)) return null;
            return row(setId);
        }

        @Override
        public List<QuestionSetRow> findAllSets(long userId) {
            if (userId != CurrentUser.DEMO_USER_ID) return List.of();
            return rows.keySet().stream().map(this::row).toList();
        }

        @Override
        public List<String> findQuestionTexts(long setId) {
            return items.getOrDefault(setId, List.of());
        }

        @Override
        public void replaceSet(long userId, long setId, String title, QuestionSourceType sourceType,
                               String sourceNote, List<String> questions) {
            replacedSetId = setId;
            QuestionSetRow base = rows.get(setId);
            rows.put(setId, new QuestionSetRow(setId, title, sourceType, sourceNote, base.archived(),
                    base.archivedAt(), base.createdAt(), LocalDateTime.now()));
            items.put(setId, new java.util.ArrayList<>(questions));
        }

        @Override
        public void updateArchivedAt(long userId, long setId, LocalDateTime archivedAt) {
            if (archivedAt == null) archivedAtBySet.remove(setId);
            else archivedAtBySet.put(setId, archivedAt);
        }
    }
}
