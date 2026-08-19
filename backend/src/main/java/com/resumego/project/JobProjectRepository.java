package com.resumego.project;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

@Repository
public class JobProjectRepository {

    private static final String SELECT_COLUMNS = """
            SELECT id, user_id, name, status, job_description_id, resume_version_id,
                   archived_at, created_at, updated_at
            FROM job_projects
            """;

    private final JdbcTemplate jdbcTemplate;

    public JobProjectRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<JobProject> findAll(long userId) {
        return jdbcTemplate.query(
                SELECT_COLUMNS + """
                        WHERE user_id = ? AND deleted_at IS NULL
                        ORDER BY CASE status WHEN 'active' THEN 0 ELSE 1 END, updated_at DESC, id DESC
                        """,
                rowMapper(),
                userId
        );
    }

    public Optional<JobProject> findById(long userId, long projectId) {
        return jdbcTemplate.query(
                SELECT_COLUMNS + " WHERE id = ? AND user_id = ? AND deleted_at IS NULL",
                rowMapper(),
                projectId,
                userId
        ).stream().findFirst();
    }

    public long create(long userId, String name, Long jobDescriptionId, Long resumeVersionId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                            INSERT INTO job_projects
                                (user_id, name, status, job_description_id, resume_version_id)
                            VALUES (?, ?, 'active', ?, ?)
                            """,
                    new String[]{"id"}
            );
            statement.setLong(1, userId);
            statement.setString(2, name);
            statement.setObject(3, jobDescriptionId);
            statement.setObject(4, resumeVersionId);
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("创建求职项目失败：未返回主键");
        }
        return key.longValue();
    }

    public int rename(long userId, long projectId, String name) {
        return jdbcTemplate.update(
                """
                        UPDATE job_projects
                        SET name = ?, updated_at = CURRENT_TIMESTAMP
                        WHERE id = ? AND user_id = ? AND deleted_at IS NULL
                        """,
                name, projectId, userId
        );
    }

    public int updateLinks(long userId, long projectId, Long jobDescriptionId, Long resumeVersionId) {
        return jdbcTemplate.update(
                """
                        UPDATE job_projects
                        SET job_description_id = ?, resume_version_id = ?, updated_at = CURRENT_TIMESTAMP
                        WHERE id = ? AND user_id = ? AND deleted_at IS NULL
                        """,
                jobDescriptionId, resumeVersionId, projectId, userId
        );
    }

    public int archive(long userId, long projectId) {
        return jdbcTemplate.update(
                """
                        UPDATE job_projects
                        SET status = 'archived', archived_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
                        WHERE id = ? AND user_id = ? AND deleted_at IS NULL AND status = 'active'
                        """,
                projectId, userId
        );
    }

    public int restore(long userId, long projectId) {
        return jdbcTemplate.update(
                """
                        UPDATE job_projects
                        SET status = 'active', archived_at = NULL, updated_at = CURRENT_TIMESTAMP
                        WHERE id = ? AND user_id = ? AND deleted_at IS NULL AND status = 'archived'
                        """,
                projectId, userId
        );
    }

    public int softDelete(long userId, long projectId) {
        return jdbcTemplate.update(
                """
                        UPDATE job_projects
                        SET deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
                        WHERE id = ? AND user_id = ? AND deleted_at IS NULL
                        """,
                projectId, userId
        );
    }

    public boolean ownsJobDescription(long userId, long jobDescriptionId) {
        return hasRows(
                "SELECT COUNT(*) FROM job_descriptions WHERE id = ? AND user_id = ?",
                jobDescriptionId,
                userId
        );
    }

    public boolean ownsResumeVersion(long userId, long resumeVersionId) {
        return hasRows(
                """
                        SELECT COUNT(*)
                        FROM resume_versions rv
                        INNER JOIN resumes r ON r.id = rv.resume_id
                        WHERE rv.id = ? AND r.user_id = ? AND r.deleted_at IS NULL
                        """,
                resumeVersionId,
                userId
        );
    }

    private boolean hasRows(String sql, Object... args) {
        Long count = jdbcTemplate.queryForObject(sql, Long.class, args);
        return count != null && count > 0;
    }

    private RowMapper<JobProject> rowMapper() {
        return (rs, rowNum) -> new JobProject(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getString("name"),
                rs.getString("status"),
                rs.getObject("job_description_id", Long.class),
                rs.getObject("resume_version_id", Long.class),
                rs.getTimestamp("archived_at") == null ? null : rs.getTimestamp("archived_at").toLocalDateTime(),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime()
        );
    }
}
