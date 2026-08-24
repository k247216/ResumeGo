package com.resumego.resume.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumego.resume.dto.ResumeVersionDTO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Map;

@Repository
public class ResumeRepository {

    public record EvidenceRefDraft(Long evidenceId, String sectionKey) {
    }

    public record ProjectEvidenceDraft(
            String title,
            String situation,
            String actionText,
            String resultText,
            List<String> skillTags,
            String sourceNote
    ) {
    }

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public ResumeRepository(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public List<Long> findIdsByUserId(long userId) {
        return jdbcTemplate.query(
                "SELECT id FROM resumes WHERE user_id = ? AND deleted_at IS NULL ORDER BY id",
                (rs, rowNum) -> rs.getLong("id"),
                userId
        );
    }

    public long insertResume(long userId, String title, Long targetJobDescriptionId) {
        jdbcTemplate.update(
                "INSERT INTO resumes (user_id, title, target_job_description_id) VALUES (?, ?, ?)",
                userId, title, targetJobDescriptionId
        );
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return id != null ? id : 0L;
    }

    public String findTitleById(long id) {
        return jdbcTemplate.queryForObject(
                "SELECT title FROM resumes WHERE id = ? AND deleted_at IS NULL",
                String.class,
                id
        );
    }

    public Long findCurrentVersionId(long resumeId) {
        return jdbcTemplate.queryForObject(
                "SELECT current_version_id FROM resumes WHERE id = ?",
                Long.class,
                resumeId
        );
    }

    public Long findTargetJobDescriptionIdById(long id) {
        return jdbcTemplate.queryForObject(
                "SELECT target_job_description_id FROM resumes WHERE id = ? AND deleted_at IS NULL",
                Long.class,
                id
        );
    }

    public void updateTargetJobDescriptionId(long resumeId, Long targetJobDescriptionId) {
        jdbcTemplate.update(
                "UPDATE resumes SET target_job_description_id = ?, updated_at = NOW(3) WHERE id = ? AND deleted_at IS NULL",
                targetJobDescriptionId, resumeId
        );
    }

    public ResumeVersionDTO findVersionById(long versionId) {
        List<ResumeVersionDTO> rows = jdbcTemplate.query(
                """
                        SELECT id, resume_id, parent_version_id, version_no, content_json,
                               change_summary, created_by_type, created_at
                        FROM resume_versions
                        WHERE id = ?
                        """,
                versionRowMapper(),
                versionId
        );
        return rows.stream().findFirst().orElse(null);
    }

    public List<ResumeVersionDTO> findVersionsByResumeId(long resumeId) {
        return jdbcTemplate.query(
                """
                        SELECT id, resume_id, parent_version_id, version_no, content_json,
                               change_summary, created_by_type, created_at
                        FROM resume_versions
                        WHERE resume_id = ?
                        ORDER BY version_no DESC
                        """,
                versionRowMapper(),
                resumeId
        );
    }

    public int findMaxVersionNo(long resumeId) {
        Integer max = jdbcTemplate.queryForObject(
                "SELECT MAX(version_no) FROM resume_versions WHERE resume_id = ?",
                Integer.class,
                resumeId
        );
        return max != null ? max : 0;
    }

    public long insertVersion(long resumeId, Long parentVersionId, int versionNo,
                              String contentJson, String changeSummary, String createdByType) {
        jdbcTemplate.update(
                """
                        INSERT INTO resume_versions
                        (resume_id, parent_version_id, version_no, content_json,
                         change_summary, created_by_type)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """,
                resumeId, parentVersionId, versionNo, contentJson, changeSummary, createdByType
        );
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        return id != null ? id : 0L;
    }

    public void updateCurrentVersionId(long resumeId, long versionId) {
        jdbcTemplate.update(
                "UPDATE resumes SET current_version_id = ?, updated_at = NOW(3) WHERE id = ?",
                versionId, resumeId
        );
    }

    public void replaceEvidenceRefsForVersion(long resumeVersionId, long userId,
                                              List<EvidenceRefDraft> evidenceRefs) {
        jdbcTemplate.update(
                "DELETE FROM resume_evidence_refs WHERE resume_version_id = ?",
                resumeVersionId
        );
        for (EvidenceRefDraft ref : evidenceRefs) {
            jdbcTemplate.update(
                    """
                            INSERT INTO resume_evidence_refs (resume_version_id, evidence_id, section_key)
                            SELECT ?, ce.id, ?
                            FROM capability_evidences ce
                            WHERE ce.id = ?
                              AND ce.user_id = ?
                              AND ce.deleted_at IS NULL
                            """,
                    resumeVersionId,
                    ref.sectionKey(),
                    ref.evidenceId(),
                    userId
            );
        }
    }

    public long createSelfReportedProjectEvidence(long userId, ProjectEvidenceDraft evidence) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    """
                            INSERT INTO capability_evidences (
                                user_id, evidence_type, title, situation, action_text,
                                result_text, skill_tags, source_note
                            )
                            VALUES (?, 'project', ?, ?, ?, ?, CAST(? AS JSON), ?)
                            """,
                    Statement.RETURN_GENERATED_KEYS
            );
            statement.setLong(1, userId);
            statement.setString(2, evidence.title());
            statement.setString(3, evidence.situation());
            statement.setString(4, evidence.actionText());
            statement.setString(5, evidence.resultText());
            statement.setString(6, toJson(evidence.skillTags()));
            statement.setString(7, evidence.sourceNote());
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("创建项目自述证据失败：未返回主键");
        }
        return key.longValue();
    }

    public boolean existsActiveEvidenceForUser(long userId, long evidenceId) {
        Integer count = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM capability_evidences
                        WHERE id = ?
                          AND user_id = ?
                          AND deleted_at IS NULL
                        """,
                Integer.class,
                evidenceId,
                userId
        );
        return count != null && count > 0;
    }

    private String toJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("项目证据技能标签格式错误", exception);
        }
    }

    /**
     * 将 suggestedText 应用到 contentJson 中 sectionKey 指定的位置。
     * <p>
     * 支持格式：fieldName、array[N]、field.array[N]、field.array[N].subField。
     * 如果目标是一个 Map（对象），则设置其 "description" 字段。
     */
    @SuppressWarnings("unchecked")
    public String applySuggestion(String contentJson, String sectionKey, String suggestedText) {
        try {
            Map<String, Object> root = objectMapper.readValue(contentJson,
                    new TypeReference<Map<String, Object>>() {});
            Object target = navigateTo(root, sectionKey);
            if (target instanceof Map) {
                ((Map<String, Object>) target).put("description", suggestedText);
            } else {
                setByPath(root, sectionKey, suggestedText);
            }
            return objectMapper.writeValueAsString(root);
        } catch (Exception e) {
            throw new IllegalStateException("应用建议到简历内容失败: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private Object navigateTo(Map<String, Object> root, String path) {
        String[] parts = path.split("\\.");
        Object current = root;
        for (String part : parts) {
            String fieldName = part;
            int idx = -1;
            int bracketPos = part.indexOf('[');
            if (bracketPos > 0) {
                fieldName = part.substring(0, bracketPos);
                String idxStr = part.substring(bracketPos + 1, part.length() - 1);
                idx = Integer.parseInt(idxStr);
            }
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(fieldName);
            }
            if (idx >= 0 && current instanceof List) {
                current = ((List<?>) current).get(idx);
            }
        }
        return current;
    }

    @SuppressWarnings("unchecked")
    private void setByPath(Map<String, Object> root, String path, Object value) {
        String[] parts = path.split("\\.");
        Object current = root;
        for (int i = 0; i < parts.length - 1; i++) {
            String fieldName = parts[i];
            int idx = -1;
            int bracketPos = fieldName.indexOf('[');
            if (bracketPos > 0) {
                fieldName = fieldName.substring(0, bracketPos);
                String idxStr = parts[i].substring(bracketPos + 1, parts[i].length() - 1);
                idx = Integer.parseInt(idxStr);
            }
            if (current instanceof Map) {
                current = ((Map<String, Object>) current).get(fieldName);
            }
            if (idx >= 0 && current instanceof List) {
                current = ((List<?>) current).get(idx);
            }
        }
        String lastKey = parts[parts.length - 1];
        int lastBracket = lastKey.indexOf('[');
        if (lastBracket > 0) {
            String fieldName = lastKey.substring(0, lastBracket);
            String idxStr = lastKey.substring(lastBracket + 1, lastKey.length() - 1);
            int idx = Integer.parseInt(idxStr);
            if (current instanceof Map) {
                List<Object> list = (List<Object>) ((Map<String, Object>) current).get(fieldName);
                if (list != null) {
                    list.set(idx, value);
                }
            }
        } else {
            if (current instanceof Map) {
                ((Map<String, Object>) current).put(lastKey, value);
            }
        }
    }

    private RowMapper<ResumeVersionDTO> versionRowMapper() {
        return (rs, rowNum) -> new ResumeVersionDTO(
                rs.getLong("id"),
                rs.getLong("resume_id"),
                rs.getObject("parent_version_id", Long.class),
                rs.getInt("version_no"),
                parseContentJson(rs.getString("content_json")),
                rs.getString("change_summary"),
                rs.getString("created_by_type"),
                rs.getTimestamp("created_at").toLocalDateTime()
        );
    }

    private Map<String, Object> parseContentJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("简历版本 content_json 解析失败", e);
        }
    }

    public int softDelete(long userId, long resumeId) {
        return jdbcTemplate.update(
                """
                UPDATE resumes
                SET deleted_at = CURRENT_TIMESTAMP, updated_at = CURRENT_TIMESTAMP
                WHERE id = ? AND user_id = ? AND deleted_at IS NULL
                """,
                resumeId, userId
        );
    }
}
