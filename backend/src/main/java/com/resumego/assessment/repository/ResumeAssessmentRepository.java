package com.resumego.assessment.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumego.assessment.dto.ResumeAssessmentEvidenceRef;
import com.resumego.assessment.dto.ResumeAssessmentResponse;
import com.resumego.assessment.dto.ResumeAssessmentResultDraft;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public class ResumeAssessmentRepository {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<Map<String, Object>>> DEDUCTION_LIST_TYPE = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public ResumeAssessmentRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public List<ResumeAssessmentEvidenceRef> findEvidenceRefs(long resumeVersionId) {
        return jdbcTemplate.query(
                """
                        SELECT rer.id, rer.evidence_id, rer.section_key,
                               CASE WHEN ce.deleted_at IS NULL THEN TRUE ELSE FALSE END AS active
                        FROM resume_evidence_refs rer
                        JOIN capability_evidences ce ON ce.id = rer.evidence_id
                        WHERE rer.resume_version_id = ?
                        ORDER BY rer.id
                        """,
                (rs, rowNum) -> new ResumeAssessmentEvidenceRef(
                        rs.getLong("id"),
                        rs.getLong("evidence_id"),
                        rs.getString("section_key"),
                        rs.getBoolean("active")
                ),
                resumeVersionId
        );
    }

    public Optional<ResumeAssessmentResponse> findByFingerprint(
            long resumeVersionId,
            String ruleVersion,
            String inputFingerprint
    ) {
        List<ResumeAssessmentResponse> rows = jdbcTemplate.query(
                """
                        SELECT id, resume_version_id, rule_version, total_score,
                               dimension_scores, deductions, input_fingerprint, created_at
                        FROM resume_assessments
                        WHERE resume_version_id = ?
                          AND rule_version = ?
                          AND input_fingerprint = ?
                        """,
                assessmentRowMapper(),
                resumeVersionId,
                ruleVersion,
                inputFingerprint
        );
        return rows.stream().findFirst();
    }

    public long create(
            long resumeVersionId,
            String ruleVersion,
            String inputFingerprint,
            ResumeAssessmentResultDraft result
    ) {
        String dimensionScoresJson = toJson(result.dimensionScores());
        String deductionsJson = toJson(result.deductions());
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                            INSERT INTO resume_assessments (
                                resume_version_id, rule_version, total_score,
                                dimension_scores, deductions, input_fingerprint
                            )
                            VALUES (?, ?, ?, CAST(? AS JSON), CAST(? AS JSON), ?)
                            """,
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setLong(1, resumeVersionId);
            statement.setString(2, ruleVersion);
            statement.setBigDecimal(3, result.totalScore());
            statement.setString(4, dimensionScoresJson);
            statement.setString(5, deductionsJson);
            statement.setString(6, inputFingerprint);
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("创建简历评分记录失败：未返回主键");
        }
        return key.longValue();
    }

    public Optional<ResumeAssessmentResponse> findByVersionId(long resumeVersionId) {
        List<ResumeAssessmentResponse> rows = jdbcTemplate.query(
                """
                        SELECT id, resume_version_id, rule_version, total_score,
                               dimension_scores, deductions, input_fingerprint, created_at
                        FROM resume_assessments
                        WHERE resume_version_id = ?
                        ORDER BY created_at DESC
                        LIMIT 1
                        """,
                assessmentRowMapper(),
                resumeVersionId
        );
        return rows.stream().findFirst();
    }

    public Optional<ResumeAssessmentResponse> findById(long id) {
        List<ResumeAssessmentResponse> rows = jdbcTemplate.query(
                """
                        SELECT id, resume_version_id, rule_version, total_score,
                               dimension_scores, deductions, input_fingerprint, created_at
                        FROM resume_assessments
                        WHERE id = ?
                        """,
                assessmentRowMapper(),
                id
        );
        return rows.stream().findFirst();
    }

    private RowMapper<ResumeAssessmentResponse> assessmentRowMapper() {
        return (rs, rowNum) -> new ResumeAssessmentResponse(
                rs.getLong("id"),
                rs.getLong("resume_version_id"),
                rs.getString("rule_version"),
                rs.getBigDecimal("total_score"),
                fromJsonMap(rs.getString("dimension_scores")),
                fromJsonDeductionList(rs.getString("deductions")),
                rs.getString("input_fingerprint"),
                rs.getTimestamp("created_at").toLocalDateTime()
        );
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("简历评分结果 JSON 序列化失败", exception);
        }
    }

    private Map<String, Object> fromJsonMap(String json) {
        try {
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("简历评分维度分解析失败", exception);
        }
    }

    private List<Map<String, Object>> fromJsonDeductionList(String json) {
        try {
            return objectMapper.readValue(json, DEDUCTION_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("简历评分扣分项解析失败", exception);
        }
    }
}
