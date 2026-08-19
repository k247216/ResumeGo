package com.resumego.interview.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumego.common.CurrentUser;
import com.resumego.interview.dto.InterviewGrowthDimensions;
import com.resumego.interview.dto.InterviewGrowthReportResponse;
import com.resumego.interview.dto.InterviewGrowthSnapshotResponse;
import com.resumego.interview.dto.MultiSessionSummaryResponse;
import com.resumego.interview.entity.InterviewPlan;
import com.resumego.interview.entity.InterviewSession;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 面试成长趋势快照服务。
 * <p>
 * 只消费已经结构化校验并保存的面试评价分数，不参与状态机转换、不生成简历评分或岗位排序。
 */
@Service
@RequiredArgsConstructor
public class InterviewGrowthService {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public void upsertSnapshotForPlan(InterviewPlan plan,
                                      List<InterviewSession> completedSessions,
                                      MultiSessionSummaryResponse summary) {
        if (plan == null || completedSessions == null || completedSessions.isEmpty()) {
            return;
        }
        List<String> scoreJsonRows = jdbcTemplate.queryForList("""
                SELECT e.score_json
                FROM interview_evaluations e
                INNER JOIN interview_sessions s ON s.id = e.session_id
                WHERE s.plan_id = ?
                ORDER BY s.round_order ASC, e.created_at ASC
                """, String.class, plan.getId());
        if (scoreJsonRows.isEmpty()) {
            return;
        }

        Long resumeId = jdbcTemplate.queryForObject(
                "SELECT resume_id FROM resume_versions WHERE id = ?",
                Long.class,
                plan.getResumeVersionId()
        );
        if (resumeId == null) {
            return;
        }

        LocalDateTime completedAt = completedSessions.stream()
                .map(InterviewSession::getCompletedAt)
                .filter(value -> value != null)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());
        SnapshotDraft draft = buildSnapshotDraft(plan, scoreJsonRows, summary, completedAt);

        jdbcTemplate.update("""
                INSERT INTO interview_growth_snapshots (
                    user_id, resume_id, resume_version_id, job_description_id, interview_plan_id,
                    clarity_score, relevance_score, depth_score, accuracy_score, overall_score,
                    weak_points_json, summary_text, completed_at
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    clarity_score = VALUES(clarity_score),
                    relevance_score = VALUES(relevance_score),
                    depth_score = VALUES(depth_score),
                    accuracy_score = VALUES(accuracy_score),
                    overall_score = VALUES(overall_score),
                    weak_points_json = VALUES(weak_points_json),
                    summary_text = VALUES(summary_text),
                    completed_at = VALUES(completed_at),
                    updated_at = CURRENT_TIMESTAMP(3)
                """,
                CurrentUser.DEMO_USER_ID,
                resumeId,
                draft.resumeVersionId(),
                draft.jobDescriptionId(),
                draft.interviewPlanId(),
                draft.clarityScore(),
                draft.relevanceScore(),
                draft.depthScore(),
                draft.accuracyScore(),
                draft.overallScore(),
                draft.weakPointsJson(),
                draft.summaryText(),
                draft.completedAt()
        );
    }

    public InterviewGrowthReportResponse getGrowthReport(Long resumeId, Long jobDescriptionId) {
        backfillMissingSnapshots(resumeId, jobDescriptionId);

        JobBrief job = loadJobBrief(jobDescriptionId);
        List<InterviewGrowthSnapshotResponse> snapshots = jdbcTemplate.query("""
                SELECT
                    s.resume_version_id,
                    CONCAT('v', rv.version_no, ' · ', CASE rv.created_by_type
                        WHEN 'user' THEN '用户编辑'
                        WHEN 'ai_suggestion' THEN 'AI优化'
                        ELSE '导入'
                    END) AS version_label,
                    s.interview_plan_id,
                    s.completed_at,
                    (
                        SELECT COUNT(*)
                        FROM interview_growth_snapshots c
                        WHERE c.user_id = s.user_id
                          AND c.resume_version_id = s.resume_version_id
                          AND c.job_description_id = s.job_description_id
                    ) AS interview_count,
                    s.clarity_score,
                    s.relevance_score,
                    s.depth_score,
                    s.accuracy_score,
                    s.summary_text
                FROM interview_growth_snapshots s
                INNER JOIN resume_versions rv ON rv.id = s.resume_version_id
                WHERE s.user_id = ?
                  AND s.resume_id = ?
                  AND s.job_description_id = ?
                ORDER BY s.completed_at ASC, s.id ASC
                """,
                (rs, rowNum) -> new InterviewGrowthSnapshotResponse(
                        rs.getLong("resume_version_id"),
                        rs.getString("version_label"),
                        rs.getLong("interview_plan_id"),
                        toLocalDateTime(rs.getTimestamp("completed_at")),
                        rs.getInt("interview_count"),
                        new InterviewGrowthDimensions(
                                rs.getDouble("clarity_score"),
                                rs.getDouble("relevance_score"),
                                rs.getDouble("depth_score"),
                                rs.getDouble("accuracy_score")
                        ),
                        rs.getString("summary_text")
                ),
                CurrentUser.DEMO_USER_ID,
                resumeId,
                jobDescriptionId
        );

        return new InterviewGrowthReportResponse(
                resumeId,
                jobDescriptionId,
                job.jobTitle(),
                job.companyName(),
                snapshots,
                buildChanges(snapshots)
        );
    }

    void backfillMissingSnapshots(Long resumeId, Long jobDescriptionId) {
        if (resumeId == null || jobDescriptionId == null) {
            return;
        }
        List<InterviewPlan> plans = jdbcTemplate.query("""
                SELECT p.id, p.user_id, p.resume_version_id, p.job_description_id,
                       p.title, p.question_count, p.persona_plan_json, p.focus_tags_json,
                       p.supplement_text, p.summary_json, p.summary_generated_at,
                       p.deleted_at, p.created_at, p.updated_at
                FROM interview_plans p
                INNER JOIN resume_versions rv ON rv.id = p.resume_version_id
                LEFT JOIN interview_growth_snapshots s ON s.interview_plan_id = p.id
                WHERE p.user_id = ?
                  AND rv.resume_id = ?
                  AND p.job_description_id = ?
                  AND p.deleted_at IS NULL
                  AND p.summary_json IS NOT NULL
                  AND s.id IS NULL
                ORDER BY p.updated_at ASC, p.id ASC
                """,
                (rs, rowNum) -> {
                    InterviewPlan plan = new InterviewPlan();
                    plan.setId(rs.getLong("id"));
                    plan.setUserId(rs.getLong("user_id"));
                    plan.setResumeVersionId(rs.getLong("resume_version_id"));
                    plan.setJobDescriptionId(rs.getLong("job_description_id"));
                    plan.setTitle(rs.getString("title"));
                    plan.setQuestionCount(rs.getInt("question_count"));
                    plan.setPersonaPlanJson(rs.getString("persona_plan_json"));
                    plan.setFocusTagsJson(rs.getString("focus_tags_json"));
                    plan.setSupplementText(rs.getString("supplement_text"));
                    plan.setSummaryJson(rs.getString("summary_json"));
                    plan.setSummaryGeneratedAt(toLocalDateTime(rs.getTimestamp("summary_generated_at")));
                    plan.setDeletedAt(toLocalDateTime(rs.getTimestamp("deleted_at")));
                    plan.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
                    plan.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
                    return plan;
                },
                CurrentUser.DEMO_USER_ID,
                resumeId,
                jobDescriptionId
        );

        for (InterviewPlan plan : plans) {
            if (!Objects.equals(plan.getUserId(), CurrentUser.DEMO_USER_ID)) {
                continue;
            }
            Long unfinishedCount = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*)
                    FROM interview_sessions
                    WHERE plan_id = ? AND status <> 'COMPLETED'
                    """, Long.class, plan.getId());
            if (unfinishedCount != null && unfinishedCount > 0) {
                continue;
            }
            List<InterviewSession> completedSessions = loadCompletedSessions(plan.getId());
            if (completedSessions.isEmpty()) {
                continue;
            }
            MultiSessionSummaryResponse summary = parseSummary(plan.getSummaryJson());
            if (summary == null) {
                continue;
            }
            upsertSnapshotForPlan(plan, completedSessions, summary);
        }
    }

    SnapshotDraft buildSnapshotDraft(InterviewPlan plan,
                                     List<String> scoreJsonRows,
                                     MultiSessionSummaryResponse summary,
                                     LocalDateTime completedAt) {
        List<Map<String, Number>> scores = scoreJsonRows.stream()
                .map(this::parseScoreJson)
                .filter(score -> !score.isEmpty())
                .toList();
        if (scores.isEmpty()) {
            throw new IllegalArgumentException("缺少可用的面试评分数据");
        }

        double clarity = average(scores, "clarity");
        double relevance = average(scores, "relevance");
        double depth = average(scores, "depth");
        double accuracy = average(scores, "accuracy");
        double overall = roundToOneDecimal((clarity + relevance + depth + accuracy) / 4.0);

        return new SnapshotDraft(
                plan.getResumeVersionId(),
                plan.getJobDescriptionId(),
                plan.getId(),
                clarity,
                relevance,
                depth,
                accuracy,
                overall,
                writeJson(summary != null ? summary.crossWeaknesses() : List.of()),
                summary != null ? summary.overallSummary() : "",
                completedAt
        );
    }

    private InterviewGrowthDimensions buildChanges(List<InterviewGrowthSnapshotResponse> snapshots) {
        if (snapshots.size() < 2) {
            return new InterviewGrowthDimensions(0, 0, 0, 0);
        }
        InterviewGrowthDimensions first = snapshots.getFirst().dimensions();
        InterviewGrowthDimensions last = snapshots.getLast().dimensions();
        return new InterviewGrowthDimensions(
                roundToOneDecimal(last.clarity() - first.clarity()),
                roundToOneDecimal(last.relevance() - first.relevance()),
                roundToOneDecimal(last.depth() - first.depth()),
                roundToOneDecimal(last.accuracy() - first.accuracy())
        );
    }

    private double average(List<Map<String, Number>> scores, String key) {
        return roundToOneDecimal(scores.stream()
                .map(score -> score.get(key))
                .filter(value -> value != null)
                .mapToDouble(Number::doubleValue)
                .average()
                .orElse(0));
    }

    private Map<String, Number> parseScoreJson(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Number>>() {});
        } catch (Exception e) {
            return Map.of();
        }
    }

    private List<InterviewSession> loadCompletedSessions(Long planId) {
        return jdbcTemplate.query("""
                SELECT id, user_id, resume_version_id, job_description_id, status,
                       current_question_index, total_questions, started_at, completed_at,
                       summary_json, persona_id, persona_name, persona_title, plan_id,
                       round_order, created_at, updated_at
                FROM interview_sessions
                WHERE plan_id = ? AND status = 'COMPLETED'
                ORDER BY round_order ASC, id ASC
                """,
                (rs, rowNum) -> {
                    InterviewSession session = new InterviewSession();
                    session.setId(rs.getLong("id"));
                    session.setUserId(rs.getLong("user_id"));
                    session.setResumeVersionId(rs.getLong("resume_version_id"));
                    session.setJobDescriptionId(rs.getLong("job_description_id"));
                    session.setStatus(rs.getString("status"));
                    session.setCurrentQuestionIndex(rs.getInt("current_question_index"));
                    session.setTotalQuestions(rs.getInt("total_questions"));
                    session.setStartedAt(toLocalDateTime(rs.getTimestamp("started_at")));
                    session.setCompletedAt(toLocalDateTime(rs.getTimestamp("completed_at")));
                    session.setSummaryJson(rs.getString("summary_json"));
                    session.setPersonaId(rs.getLong("persona_id"));
                    session.setPersonaName(rs.getString("persona_name"));
                    session.setPersonaTitle(rs.getString("persona_title"));
                    session.setPlanId(rs.getLong("plan_id"));
                    session.setRoundOrder(rs.getInt("round_order"));
                    session.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
                    session.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
                    return session;
                },
                planId
        );
    }

    private MultiSessionSummaryResponse parseSummary(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, MultiSessionSummaryResponse.class);
        } catch (Exception e) {
            return null;
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "[]";
        }
    }

    private JobBrief loadJobBrief(Long jobDescriptionId) {
        try {
            return jdbcTemplate.queryForObject("""
                    SELECT job_title, company_name
                    FROM job_descriptions
                    WHERE id = ? AND user_id = ?
                    """,
                    (rs, rowNum) -> new JobBrief(
                            rs.getString("job_title"),
                            rs.getString("company_name")
                    ),
                    jobDescriptionId,
                    CurrentUser.DEMO_USER_ID
            );
        } catch (EmptyResultDataAccessException e) {
            return new JobBrief("岗位 #" + jobDescriptionId, "");
        }
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private double roundToOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    record SnapshotDraft(
            Long resumeVersionId,
            Long jobDescriptionId,
            Long interviewPlanId,
            double clarityScore,
            double relevanceScore,
            double depthScore,
            double accuracyScore,
            double overallScore,
            String weakPointsJson,
            String summaryText,
            LocalDateTime completedAt
    ) {
    }

    private record JobBrief(String jobTitle, String companyName) {
    }
}
