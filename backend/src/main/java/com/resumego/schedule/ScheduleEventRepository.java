package com.resumego.schedule;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class ScheduleEventRepository {

    private static final String SELECT_COLUMNS = """
            SELECT id, user_id, title, event_type, start_time, end_time, notes,
                   job_description_id, created_at, updated_at
            FROM schedule_events
            """;

    private final JdbcTemplate jdbcTemplate;

    public ScheduleEventRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ScheduleEvent> findAll(long userId, LocalDateTime from, LocalDateTime to) {
        StringBuilder sql = new StringBuilder(SELECT_COLUMNS).append(" WHERE user_id = ? AND deleted_at IS NULL");
        List<Object> args = new ArrayList<>();
        args.add(userId);
        if (from != null) {
            sql.append(" AND start_time >= ?");
            args.add(Timestamp.valueOf(from));
        }
        if (to != null) {
            sql.append(" AND start_time < ?");
            args.add(Timestamp.valueOf(to));
        }
        sql.append(" ORDER BY start_time ASC, id ASC");
        return jdbcTemplate.query(sql.toString(), rowMapper(), args.toArray());
    }

    public Optional<ScheduleEvent> findById(long userId, long eventId) {
        return jdbcTemplate.query(
                SELECT_COLUMNS + " WHERE id = ? AND user_id = ? AND deleted_at IS NULL",
                rowMapper(),
                eventId,
                userId
        ).stream().findFirst();
    }

    public long create(long userId, String title, String eventType, LocalDateTime startTime,
                       LocalDateTime endTime, String notes, Long jobDescriptionId) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                            INSERT INTO schedule_events
                                (user_id, title, event_type, start_time, end_time, notes, job_description_id)
                            VALUES (?, ?, ?, ?, ?, ?, ?)
                            """,
                    new String[]{"id"}
            );
            statement.setLong(1, userId);
            statement.setString(2, title);
            statement.setString(3, eventType);
            statement.setTimestamp(4, Timestamp.valueOf(startTime));
            if (endTime == null) {
                statement.setNull(5, java.sql.Types.TIMESTAMP);
            } else {
                statement.setTimestamp(5, Timestamp.valueOf(endTime));
            }
            statement.setString(6, notes);
            statement.setObject(7, jobDescriptionId);
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("创建日程失败：未返回主键");
        }
        return key.longValue();
    }

    public int update(long userId, long eventId, String title, String eventType, LocalDateTime startTime,
                      LocalDateTime endTime, String notes, Long jobDescriptionId) {
        return jdbcTemplate.update(
                """
                        UPDATE schedule_events
                        SET title = ?, event_type = ?, start_time = ?, end_time = ?, notes = ?,
                            job_description_id = ?, updated_at = CURRENT_TIMESTAMP
                        WHERE id = ? AND user_id = ? AND deleted_at IS NULL
                        """,
                title, eventType, Timestamp.valueOf(startTime),
                endTime == null ? null : Timestamp.valueOf(endTime),
                notes, jobDescriptionId, eventId, userId
        );
    }

    public int softDelete(long userId, long eventId) {
        return jdbcTemplate.update(
                """
                        UPDATE schedule_events
                        SET deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
                        WHERE id = ? AND user_id = ? AND deleted_at IS NULL
                        """,
                eventId, userId
        );
    }

    public boolean ownsJobDescription(long userId, long jobDescriptionId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM job_descriptions WHERE id = ? AND user_id = ?",
                Long.class, jobDescriptionId, userId);
        return count != null && count > 0;
    }

    private RowMapper<ScheduleEvent> rowMapper() {
        return (rs, rowNum) -> new ScheduleEvent(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getString("title"),
                rs.getString("event_type"),
                rs.getTimestamp("start_time").toLocalDateTime(),
                rs.getTimestamp("end_time") == null ? null : rs.getTimestamp("end_time").toLocalDateTime(),
                rs.getString("notes"),
                rs.getObject("job_description_id", Long.class),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime()
        );
    }
}
