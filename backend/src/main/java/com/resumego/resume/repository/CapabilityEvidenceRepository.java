package com.resumego.resume.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumego.resume.dto.CapabilityEvidenceCreateRequest;
import com.resumego.resume.dto.CapabilityEvidenceResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;

@Repository
public class CapabilityEvidenceRepository {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public CapabilityEvidenceRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public List<CapabilityEvidenceResponse> findActiveByUserId(long userId) {
        return jdbcTemplate.query(
                """
                        SELECT id, user_id, evidence_type, title, situation, action_text, result_text,
                               skill_tags, source_note, created_at, updated_at
                        FROM capability_evidences
                        WHERE user_id = ? AND deleted_at IS NULL
                        ORDER BY id
                        """,
                evidenceRowMapper(),
                userId
        );
    }

    public Optional<CapabilityEvidenceResponse> findActiveByIdAndUserId(long id, long userId) {
        List<CapabilityEvidenceResponse> rows = jdbcTemplate.query(
                """
                        SELECT id, user_id, evidence_type, title, situation, action_text, result_text,
                               skill_tags, source_note, created_at, updated_at
                        FROM capability_evidences
                        WHERE id = ? AND user_id = ? AND deleted_at IS NULL
                        """,
                evidenceRowMapper(),
                id,
                userId
        );
        return rows.stream().findFirst();
    }

    public long create(long userId, CapabilityEvidenceCreateRequest request) {
        String skillTagsJson = toJson(request.skillTags());
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                            INSERT INTO capability_evidences (
                                user_id, evidence_type, title, situation, action_text,
                                result_text, skill_tags, source_note
                            )
                            VALUES (?, ?, ?, ?, ?, ?, CAST(? AS JSON), ?)
                            """,
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setLong(1, userId);
            statement.setString(2, request.evidenceType());
            statement.setString(3, request.title());
            statement.setString(4, request.situation());
            statement.setString(5, request.actionText());
            statement.setString(6, request.resultText());
            statement.setString(7, skillTagsJson);
            statement.setString(8, request.sourceNote());
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("创建能力证据失败：未返回主键");
        }
        return key.longValue();
    }

    private RowMapper<CapabilityEvidenceResponse> evidenceRowMapper() {
        return (rs, rowNum) -> new CapabilityEvidenceResponse(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getString("evidence_type"),
                rs.getString("title"),
                rs.getString("situation"),
                rs.getString("action_text"),
                rs.getString("result_text"),
                fromJsonList(rs.getString("skill_tags")),
                rs.getString("source_note"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getTimestamp("updated_at").toLocalDateTime()
        );
    }

    private String toJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("技能标签格式错误");
        }
    }

    private List<String> fromJsonList(String json) {
        try {
            return objectMapper.readValue(json, STRING_LIST_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("能力证据技能标签解析失败", exception);
        }
    }
}
