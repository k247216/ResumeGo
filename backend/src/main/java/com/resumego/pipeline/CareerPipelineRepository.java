package com.resumego.pipeline;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

@Repository
public class CareerPipelineRepository {

    private static final String PIPELINE_COLUMNS = """
            SELECT id, user_id, name, company_name, role_title, job_description_id,
                   resume_version_id, lifecycle, outcome, current_stage_id, archived_at,
                   created_at, updated_at
            FROM career_pipelines
            """;

    private final JdbcTemplate jdbcTemplate;

    public CareerPipelineRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<CareerPipeline> findAll(long userId) {
        return jdbcTemplate.query(PIPELINE_COLUMNS + """
                WHERE user_id = ?
                ORDER BY updated_at DESC, id DESC
                """, pipelineMapper(), userId);
    }

    public Optional<CareerPipeline> findById(long userId, long pipelineId) {
        return jdbcTemplate.query(PIPELINE_COLUMNS + " WHERE id = ? AND user_id = ?",
                pipelineMapper(), pipelineId, userId).stream().findFirst();
    }

    public long createPipeline(long userId, String name, String companyName, String roleTitle,
                               Long jobDescriptionId, Long resumeVersionId) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO career_pipelines
                        (user_id, name, company_name, role_title, job_description_id,
                         resume_version_id, lifecycle)
                    VALUES (?, ?, ?, ?, ?, ?, 'ACTIVE')
                    """, new String[]{"id"});
            statement.setLong(1, userId);
            statement.setString(2, name);
            statement.setString(3, companyName);
            statement.setString(4, roleTitle);
            statement.setObject(5, jobDescriptionId);
            statement.setObject(6, resumeVersionId);
            return statement;
        }, keys);
        return requiredKey(keys, "创建求职管线");
    }

    public long createStage(long pipelineId, String name, int position, PipelineStageState state) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO pipeline_stages (pipeline_id, name, position_index, state)
                    VALUES (?, ?, ?, ?)
                    """, new String[]{"id"});
            statement.setLong(1, pipelineId);
            statement.setString(2, name);
            statement.setInt(3, position);
            statement.setString(4, state.name());
            return statement;
        }, keys);
        return requiredKey(keys, "创建管线阶段");
    }

    public List<PipelineStage> findStages(long userId, long pipelineId) {
        return jdbcTemplate.query("""
                SELECT s.id, s.pipeline_id, s.name, s.position_index, s.state,
                       s.created_at, s.updated_at
                FROM pipeline_stages s
                INNER JOIN career_pipelines p ON p.id = s.pipeline_id
                WHERE s.pipeline_id = ? AND p.user_id = ?
                ORDER BY s.position_index, s.id
                """, stageMapper(), pipelineId, userId);
    }

    public Optional<PipelineStage> findStage(long userId, long pipelineId, long stageId) {
        return jdbcTemplate.query("""
                SELECT s.id, s.pipeline_id, s.name, s.position_index, s.state,
                       s.created_at, s.updated_at
                FROM pipeline_stages s
                INNER JOIN career_pipelines p ON p.id = s.pipeline_id
                WHERE s.id = ? AND s.pipeline_id = ? AND p.user_id = ?
                """, stageMapper(), stageId, pipelineId, userId).stream().findFirst();
    }

    public int nextStagePosition(long userId, long pipelineId) {
        Integer value = jdbcTemplate.queryForObject("""
                SELECT COALESCE(MAX(s.position_index), -1) + 1
                FROM pipeline_stages s
                INNER JOIN career_pipelines p ON p.id = s.pipeline_id
                WHERE s.pipeline_id = ? AND p.user_id = ?
                """, Integer.class, pipelineId, userId);
        return value == null ? 0 : value;
    }

    public int renameStage(long pipelineId, long stageId, String name) {
        return jdbcTemplate.update("""
                UPDATE pipeline_stages
                SET name = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND pipeline_id = ?
                """, name, stageId, pipelineId);
    }

    public void reorderStages(long pipelineId, List<Long> stageIds) {
        for (int index = 0; index < stageIds.size(); index++) {
            jdbcTemplate.update("""
                    UPDATE pipeline_stages
                    SET position_index = ?, updated_at = CURRENT_TIMESTAMP
                    WHERE id = ? AND pipeline_id = ?
                    """, 1_000_000 + index, stageIds.get(index), pipelineId);
        }
        for (int index = 0; index < stageIds.size(); index++) {
            jdbcTemplate.update("""
                    UPDATE pipeline_stages
                    SET position_index = ?, updated_at = CURRENT_TIMESTAMP
                    WHERE id = ? AND pipeline_id = ?
                    """, index, stageIds.get(index), pipelineId);
        }
    }

    public List<Long> findScheduleEventIds(long pipelineId) {
        return jdbcTemplate.queryForList("""
                SELECT link.schedule_event_id
                FROM pipeline_schedule_events link
                INNER JOIN schedule_events event ON event.id = link.schedule_event_id
                WHERE link.pipeline_id = ? AND event.deleted_at IS NULL
                ORDER BY link.linked_at, link.schedule_event_id
                """, Long.class, pipelineId);
    }

    public List<Long> findInterviewPlanIds(long pipelineId) {
        return jdbcTemplate.queryForList("""
                SELECT link.interview_plan_id
                FROM pipeline_interview_plans link
                INNER JOIN interview_plans plan ON plan.id = link.interview_plan_id
                WHERE link.pipeline_id = ? AND plan.deleted_at IS NULL
                ORDER BY link.linked_at, link.interview_plan_id
                """, Long.class, pipelineId);
    }

    public void replaceScheduleEventLink(long pipelineId, long eventId) {
        jdbcTemplate.update("DELETE FROM pipeline_schedule_events WHERE schedule_event_id = ?", eventId);
        jdbcTemplate.update("""
                INSERT INTO pipeline_schedule_events (pipeline_id, schedule_event_id)
                VALUES (?, ?)
                """, pipelineId, eventId);
    }

    public void replaceInterviewPlanLink(long pipelineId, long planId) {
        jdbcTemplate.update("DELETE FROM pipeline_interview_plans WHERE interview_plan_id = ?", planId);
        jdbcTemplate.update("""
                INSERT INTO pipeline_interview_plans (pipeline_id, interview_plan_id)
                VALUES (?, ?)
                """, pipelineId, planId);
    }

    public int unlinkScheduleEvent(long pipelineId, long eventId) {
        return jdbcTemplate.update("""
                DELETE FROM pipeline_schedule_events
                WHERE pipeline_id = ? AND schedule_event_id = ?
                """, pipelineId, eventId);
    }

    public int unlinkInterviewPlan(long pipelineId, long planId) {
        return jdbcTemplate.update("""
                DELETE FROM pipeline_interview_plans
                WHERE pipeline_id = ? AND interview_plan_id = ?
                """, pipelineId, planId);
    }

    public int setCurrentStage(long userId, long pipelineId, long stageId) {
        return jdbcTemplate.update("""
                UPDATE career_pipelines
                SET current_stage_id = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND user_id = ?
                """, stageId, pipelineId, userId);
    }

    public int updateStageState(long pipelineId, long stageId, PipelineStageState state) {
        return jdbcTemplate.update("""
                UPDATE pipeline_stages
                SET state = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND pipeline_id = ?
                """, state.name(), stageId, pipelineId);
    }

    public long appendTransition(long pipelineId, Long fromStageId, long toStageId,
                                 String actor, String note) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO pipeline_stage_transitions
                        (pipeline_id, from_stage_id, to_stage_id, actor, note)
                    VALUES (?, ?, ?, ?, ?)
                    """, new String[]{"id"});
            statement.setLong(1, pipelineId);
            statement.setObject(2, fromStageId);
            statement.setLong(3, toStageId);
            statement.setString(4, actor);
            statement.setString(5, note);
            return statement;
        }, keys);
        return requiredKey(keys, "记录阶段变化");
    }

    public List<PipelineStageTransition> findTransitions(long userId, long pipelineId) {
        return jdbcTemplate.query("""
                SELECT t.id, t.pipeline_id, t.from_stage_id, t.to_stage_id,
                       t.actor, t.note, t.occurred_at
                FROM pipeline_stage_transitions t
                INNER JOIN career_pipelines p ON p.id = t.pipeline_id
                WHERE t.pipeline_id = ? AND p.user_id = ?
                ORDER BY t.occurred_at, t.id
                """, (rs, rowNum) -> new PipelineStageTransition(
                rs.getLong("id"), rs.getLong("pipeline_id"),
                rs.getObject("from_stage_id", Long.class), rs.getLong("to_stage_id"),
                rs.getString("actor"), rs.getString("note"),
                rs.getTimestamp("occurred_at").toLocalDateTime()), pipelineId, userId);
    }

    public int updateLifecycle(long userId, long pipelineId, PipelineLifecycle lifecycle,
                               PipelineOutcome outcome) {
        return jdbcTemplate.update("""
                UPDATE career_pipelines
                SET lifecycle = ?, outcome = ?,
                    archived_at = CASE WHEN ? = 'ARCHIVED' THEN CURRENT_TIMESTAMP ELSE NULL END,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND user_id = ?
                """, lifecycle.name(), outcome == null ? null : outcome.name(), lifecycle.name(),
                pipelineId, userId);
    }

    public boolean ownsJobDescription(long userId, long jobDescriptionId) {
        return count("SELECT COUNT(*) FROM job_descriptions WHERE id = ? AND user_id = ?",
                jobDescriptionId, userId) > 0;
    }

    public boolean ownsResumeVersion(long userId, long resumeVersionId) {
        return count("""
                SELECT COUNT(*) FROM resume_versions rv
                INNER JOIN resumes r ON r.id = rv.resume_id
                WHERE rv.id = ? AND r.user_id = ? AND r.deleted_at IS NULL
                """, resumeVersionId, userId) > 0;
    }

    private long count(String sql, Object... args) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class, args);
        return value == null ? 0 : value;
    }

    private long requiredKey(KeyHolder keys, String action) {
        Number key = keys.getKey();
        if (key == null) throw new IllegalStateException(action + "失败：未返回主键");
        return key.longValue();
    }

    private RowMapper<CareerPipeline> pipelineMapper() {
        return (rs, rowNum) -> new CareerPipeline(
                rs.getLong("id"), rs.getLong("user_id"), rs.getString("name"),
                rs.getString("company_name"), rs.getString("role_title"),
                rs.getObject("job_description_id", Long.class),
                rs.getObject("resume_version_id", Long.class),
                PipelineLifecycle.valueOf(rs.getString("lifecycle")),
                rs.getString("outcome") == null ? null : PipelineOutcome.valueOf(rs.getString("outcome")),
                rs.getObject("current_stage_id", Long.class),
                rs.getTimestamp("archived_at") == null ? null : rs.getTimestamp("archived_at").toLocalDateTime(),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime());
    }

    private RowMapper<PipelineStage> stageMapper() {
        return (rs, rowNum) -> new PipelineStage(
                rs.getLong("id"), rs.getLong("pipeline_id"), rs.getString("name"),
                rs.getInt("position_index"), PipelineStageState.valueOf(rs.getString("state")),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime());
    }
}
