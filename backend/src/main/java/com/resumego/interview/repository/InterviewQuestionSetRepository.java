package com.resumego.interview.repository;

import com.resumego.interview.InterviewMode;
import com.resumego.interview.QuestionSourceType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Types;
import java.util.List;
import java.util.Optional;

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
            String companyName,
            String targetRole,
            String companyIconKey,
            Long sourceDocumentId,
            int questionCount,
            boolean archived,
            java.time.LocalDateTime archivedAt,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime updatedAt
    ) {
        public QuestionSetRow(long id, String title, QuestionSourceType sourceType,
                              String sourceNote, boolean archived,
                              java.time.LocalDateTime archivedAt,
                              java.time.LocalDateTime createdAt,
                              java.time.LocalDateTime updatedAt) {
            this(id, title, sourceType, sourceNote, null, null, null, null, 0,
                    archived, archivedAt, createdAt, updatedAt);
        }

        /** 兼容已存在的上下文元数据测试/调用方；来源文档关系默认为空。 */
        public QuestionSetRow(long id, String title, QuestionSourceType sourceType,
                              String sourceNote, String companyName, String targetRole,
                              String companyIconKey, int questionCount, boolean archived,
                              java.time.LocalDateTime archivedAt,
                              java.time.LocalDateTime createdAt,
                              java.time.LocalDateTime updatedAt) {
            this(id, title, sourceType, sourceNote, companyName, targetRole, companyIconKey,
                    null, questionCount, archived, archivedAt, createdAt, updatedAt);
        }
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
        return createSet(userId, title, sourceType, sourceNote, null, null, null, questions);
    }

    public long createSet(long userId, String title, QuestionSourceType sourceType,
                          String sourceNote, String companyName, String targetRole,
                          String companyIconKey, List<String> questions) {
        return createSet(userId, title, sourceType, sourceNote, companyName, targetRole,
                companyIconKey, null, questions);
    }

    /** 创建题集并保留其知识库原始资料关系；sourceDocumentId 可为空。 */
    public long createSet(long userId, String title, QuestionSourceType sourceType,
                          String sourceNote, String companyName, String targetRole,
                          String companyIconKey, Long sourceDocumentId, List<String> questions) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "INSERT INTO interview_question_sets (user_id, title, source_type, source_note, company_name, target_role, company_icon_key, source_document_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    new String[]{"id"}
            );
            statement.setLong(1, userId);
            statement.setString(2, title);
            statement.setString(3, sourceType.name());
            statement.setString(4, sourceNote);
            statement.setString(5, companyName);
            statement.setString(6, targetRole);
            statement.setString(7, companyIconKey);
            if (sourceDocumentId == null) statement.setNull(8, Types.BIGINT);
            else statement.setLong(8, sourceDocumentId);
            return statement;
        }, keyHolder);
        // 不依赖 JDBC 驱动返回的列名（MySQL 可能返回 GENERATED_KEY 而不是 id）。
        Number key = keyHolder.getKey();
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

    /** 当前用户从同一知识库资料物化的题集；用户隔离由复合条件保证。 */
    public Optional<Long> findSetIdBySourceDocument(long userId, long sourceDocumentId) {
        return jdbcTemplate.query(
                "SELECT id FROM interview_question_sets WHERE user_id = ? AND source_document_id = ? ORDER BY id DESC LIMIT 1",
                (rs, rowNum) -> rs.getLong("id"), userId, sourceDocumentId
        ).stream().findFirst();
    }

    public QuestionSetRow findSetById(long userId, long setId) {
        List<QuestionSetRow> rows = jdbcTemplate.query(
                """
                        SELECT s.id, s.title, s.source_type, s.source_note, s.company_name, s.target_role,
                               s.company_icon_key, s.source_document_id,
                               (SELECT COUNT(*) FROM interview_question_set_items i WHERE i.set_id = s.id) AS question_count,
                               s.archived_at, s.created_at, s.updated_at
                        FROM interview_question_sets
                        s
                        WHERE s.id = ? AND s.user_id = ?
                        """,
                (rs, rowNum) -> new QuestionSetRow(
                        rs.getLong("id"),
                        rs.getString("title"),
                        QuestionSourceType.valueOf(rs.getString("source_type")),
                        rs.getString("source_note"),
                        rs.getString("company_name"),
                        rs.getString("target_role"),
                        rs.getString("company_icon_key"),
                        rs.getObject("source_document_id") == null ? null : rs.getLong("source_document_id"),
                        rs.getInt("question_count"),
                        rs.getTimestamp("archived_at") != null,
                        rs.getTimestamp("archived_at") != null ? rs.getTimestamp("archived_at").toLocalDateTime() : null,
                        rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null,
                        rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null
                ),
                setId, userId
        );
        return rows.stream().findFirst().orElse(null);
    }

    public List<QuestionSetRow> findAllSets(long userId) {
        return jdbcTemplate.query(
                """
                        SELECT s.id, s.title, s.source_type, s.source_note, s.company_name, s.target_role,
                               s.company_icon_key, s.source_document_id,
                               (SELECT COUNT(*) FROM interview_question_set_items i WHERE i.set_id = s.id) AS question_count,
                               s.archived_at, s.created_at, s.updated_at
                        FROM interview_question_sets
                        s
                        WHERE s.user_id = ?
                        ORDER BY s.id DESC
                        """,
                (rs, rowNum) -> new QuestionSetRow(
                        rs.getLong("id"),
                        rs.getString("title"),
                        QuestionSourceType.valueOf(rs.getString("source_type")),
                        rs.getString("source_note"),
                        rs.getString("company_name"),
                        rs.getString("target_role"),
                        rs.getString("company_icon_key"),
                        rs.getObject("source_document_id") == null ? null : rs.getLong("source_document_id"),
                        rs.getInt("question_count"),
                        rs.getTimestamp("archived_at") != null,
                        rs.getTimestamp("archived_at") != null ? rs.getTimestamp("archived_at").toLocalDateTime() : null,
                        rs.getTimestamp("created_at") != null ? rs.getTimestamp("created_at").toLocalDateTime() : null,
                        rs.getTimestamp("updated_at") != null ? rs.getTimestamp("updated_at").toLocalDateTime() : null
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

    /** 原子替换元数据与题目（先删后插），需在事务内调用。 */
    public void replaceSet(long userId, long setId, String title, QuestionSourceType sourceType,
                           String sourceNote, List<String> questions) {
        replaceSet(userId, setId, title, sourceType, sourceNote, null, null, null, questions);
    }

    public void replaceSet(long userId, long setId, String title, QuestionSourceType sourceType,
                           String sourceNote, String companyName, String targetRole,
                           String companyIconKey, List<String> questions) {
        jdbcTemplate.update(
                "UPDATE interview_question_sets SET title = ?, source_type = ?, source_note = ?, company_name = ?, target_role = ?, company_icon_key = ?, updated_at = NOW(3) "
                        + "WHERE id = ? AND user_id = ?",
                title, sourceType.name(), sourceNote, companyName, targetRole, companyIconKey, setId, userId
        );
        jdbcTemplate.update("DELETE FROM interview_question_set_items WHERE set_id = ?", setId);
        for (int index = 0; index < questions.size(); index++) {
            jdbcTemplate.update(
                    "INSERT INTO interview_question_set_items (set_id, position_index, question_text) VALUES (?, ?, ?)",
                    setId, index, questions.get(index)
            );
        }
    }

    /** 归档：写入时间戳；恢复传 null。 */
    public void updateArchivedAt(long userId, long setId, java.time.LocalDateTime archivedAt) {
        jdbcTemplate.update(
                "UPDATE interview_question_sets SET archived_at = ?, updated_at = NOW(3) "
                        + "WHERE id = ? AND user_id = ?",
                archivedAt, setId, userId
        );
    }
}
