package com.resumego.interview.repository;

import com.resumego.interview.InterviewMode;
import com.resumego.interview.QuestionSourceType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

/**
 * 三模式契约与面经题集资产的持久化。所有读取按 user_id 隔离，跨用户按不存在处理。
 */
@Repository
public class InterviewQuestionSetRepository {

    public record QuestionSetRow(
            long id,
            String title,
            QuestionSourceType sourceType,
            String sourceNote,
            boolean archived
    ) {
    }

    private final JdbcTemplate jdbcTemplate;

    public InterviewQuestionSetRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public InterviewMode findPlanMode(long planId) {
        String value = jdbcTemplate.queryForObject(
                "SELECT mode FROM interview_plans WHERE id = ? AND deleted_at IS NULL",
                String.class,
                planId
        );
        return InterviewMode.valueOf(value);
    }

    public String findPlanSnapshot(long planId) {
        return jdbcTemplate.queryForObject(
                "SELECT start_context_snapshot_json FROM interview_plans WHERE id = ? AND deleted_at IS NULL",
                String.class,
                planId
        );
    }

    /** 创建题集与有序题目，返回题集 ID；需在事务内调用。 */
    public long createSet(long userId, String title, QuestionSourceType sourceType,
                          String sourceNote, List<String> questions) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO interview_question_sets (user_id, title, source_type, source_note) VALUES (?, ?, ?, ?)",
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setLong(1, userId);
            statement.setString(2, title);
            statement.setString(3, sourceType.name());
            statement.setString(4, sourceNote);
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKeys() == null ? null : keyHolder.getKeys().entrySet().stream()
                .filter(entry -> entry.getKey().equalsIgnoreCase("id"))
                .map(entry -> (Number) entry.getValue())
                .findFirst()
                .orElse(null);
        if (key == null) {
            throw new IllegalStateException("创建面经题集失败：未返回主键");
        }
        long setId = key.longValue();
        for (int index = 0; index < questions.size(); index++) {
            jdbcTemplate.update(
                    "INSERT INTO interview_question_set_items (set_id, position_index, question_text) VALUES (?, ?, ?)",
                    setId, index, questions.get(index)
            );
        }
        return setId;
    }

    public QuestionSetRow findSetById(long userId, long setId) {
        List<QuestionSetRow> rows = jdbcTemplate.query(
                """
                        SELECT id, title, source_type, source_note, archived_at
                        FROM interview_question_sets
                        WHERE id = ? AND user_id = ?
                        """,
                (rs, rowNum) -> new QuestionSetRow(
                        rs.getLong("id"),
                        rs.getString("title"),
                        QuestionSourceType.valueOf(rs.getString("source_type")),
                        rs.getString("source_note"),
                        rs.getTimestamp("archived_at") != null
                ),
                setId, userId
        );
        return rows.stream().findFirst().orElse(null);
    }

    public List<QuestionSetRow> findAllSets(long userId) {
        return jdbcTemplate.query(
                """
                        SELECT id, title, source_type, source_note, archived_at
                        FROM interview_question_sets
                        WHERE user_id = ?
                        ORDER BY id DESC
                        """,
                (rs, rowNum) -> new QuestionSetRow(
                        rs.getLong("id"),
                        rs.getString("title"),
                        QuestionSourceType.valueOf(rs.getString("source_type")),
                        rs.getString("source_note"),
                        rs.getTimestamp("archived_at") != null
                ),
                userId
        );
    }

    /** 按位置顺序返回题目文本。 */
    public List<String> findQuestionTexts(long setId) {
        return jdbcTemplate.queryForList(
                "SELECT question_text FROM interview_question_set_items WHERE set_id = ? ORDER BY position_index",
                String.class,
                setId
        );
    }
}
