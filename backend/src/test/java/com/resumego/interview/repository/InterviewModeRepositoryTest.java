package com.resumego.interview.repository;

import com.resumego.interview.InterviewMode;
import com.resumego.interview.QuestionSourceType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** 三模式契约与题集资产持久化测试：mode 回填、快照可读、题集顺序与越权隔离。 */
@JdbcTest
@Import(InterviewQuestionSetRepository.class)
@Sql(scripts = "/sql/interview_modes_schema.sql")
class InterviewModeRepositoryTest {

    @Autowired
    private InterviewQuestionSetRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void legacyPlansAreBackfilledToRoleBasedWithCompatibleSnapshot() {
        Map<String, Object> plan = jdbcTemplate.queryForMap(
                "SELECT mode, context_contract_version, start_context_snapshot_json FROM interview_plans WHERE user_id = 1");

        assertThat(plan.get("mode")).isEqualTo("ROLE_BASED");
        assertThat(String.valueOf(plan.get("context_contract_version"))).isEqualTo("1");
        String snapshot = String.valueOf(plan.get("start_context_snapshot_json"));
        assertThat(snapshot).contains("ROLE_BASED");
        assertThat(snapshot).contains("10");
        assertThat(snapshot).contains("100");
        // 兼容快照不复制简历/JD 正文
        assertThat(snapshot).doesNotContain("content");
    }

    @Test
    void modeAndSnapshotAreReadableThroughRepository() {
        assertThat(repository.findPlanMode(1L)).isEqualTo(InterviewMode.ROLE_BASED);
        assertThat(repository.findPlanSnapshot(1L)).contains("ROLE_BASED");
    }

    @Test
    void questionSetKeepsStableOrderedItems() {
        long setId = repository.createSet(
                1L, "腾讯面经", QuestionSourceType.IMPORTED_EXPERIENCE, "来自牛客 2026 春招",
                List.of("讲讲 JVM 内存结构", "Redis 持久化机制", "MySQL 索引下推"));

        InterviewQuestionSetRepository.QuestionSetRow meta = repository.findSetById(1L, setId);
        assertThat(meta).isNotNull();
        assertThat(meta.title()).isEqualTo("腾讯面经");
        assertThat(meta.sourceType()).isEqualTo(QuestionSourceType.IMPORTED_EXPERIENCE);
        assertThat(meta.companyName()).isNull();
        assertThat(meta.questionCount()).isEqualTo(3);

        List<String> questions = repository.findQuestionTexts(setId);
        assertThat(questions).containsExactly("讲讲 JVM 内存结构", "Redis 持久化机制", "MySQL 索引下推");
        assertThat(repository.findQuestionTexts(setId)).isEqualTo(questions);
    }

    @Test
    void questionSetStoresContextMetadataAndCount() {
        long setId = repository.createSet(
                1L, "腾讯技术一面", QuestionSourceType.IMPORTED_EXPERIENCE, "用户手动整理",
                "腾讯", "Java 后端实习", "tencent", List.of("题一", "题二"));

        InterviewQuestionSetRepository.QuestionSetRow row = repository.findSetById(1L, setId);
        assertThat(row.companyName()).isEqualTo("腾讯");
        assertThat(row.targetRole()).isEqualTo("Java 后端实习");
        assertThat(row.companyIconKey()).isEqualTo("tencent");
        assertThat(row.questionCount()).isEqualTo(2);
    }

    @Test
    void questionSetsAreInvisibleAcrossUsers() {
        long setId = repository.createSet(1L, "我的题集", QuestionSourceType.USER_MANUAL, null, List.of("题目一"));
        assertThat(repository.findSetById(1L, setId)).isNotNull();
        assertThat(repository.findSetById(2L, setId)).isNull();
        assertThat(repository.findAllSets(2L)).isEmpty();
    }

    @Test
    void invalidModeIsRejectedByCheckConstraint() {
        assertThatThrownBy(() ->
                jdbcTemplate.update("UPDATE interview_plans SET mode = 'VOICE_MODE' WHERE user_id = 1"))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    void invalidSourceTypeIsRejectedByCheckConstraint() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO interview_question_sets (user_id, title, source_type) VALUES (1, 'x', 'FAKE_SOURCE')"))
                .isInstanceOf(DataAccessException.class);
    }
}
