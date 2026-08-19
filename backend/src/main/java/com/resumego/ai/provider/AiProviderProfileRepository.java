package com.resumego.ai.provider;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class AiProviderProfileRepository {

    private static final String COLUMNS = """
            SELECT id, user_id, display_name, protocol_type, base_url, default_model,
                   is_default, last_tested_at, last_test_status, last_test_message,
                   created_at, updated_at
            FROM ai_provider_profiles
            """;

    private final JdbcTemplate jdbcTemplate;

    public AiProviderProfileRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<AiProviderProfile> findAll(long userId) {
        return jdbcTemplate.query(COLUMNS + " WHERE user_id = ? ORDER BY is_default DESC, updated_at DESC, id DESC",
                mapper(), userId);
    }

    public Optional<AiProviderProfile> findById(long userId, long id) {
        return jdbcTemplate.query(COLUMNS + " WHERE user_id = ? AND id = ?", mapper(), userId, id)
                .stream().findFirst();
    }

    public Optional<AiProviderProfile> findDefault(long userId) {
        return jdbcTemplate.query(COLUMNS + " WHERE user_id = ? AND is_default = TRUE ORDER BY id DESC",
                mapper(), userId).stream().findFirst();
    }

    public long create(long userId, AiProviderProfileRequest request) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO ai_provider_profiles
                        (user_id, display_name, protocol_type, base_url, default_model, is_default)
                    VALUES (?, ?, ?, ?, ?, FALSE)
                    """, new String[]{"id"});
            statement.setLong(1, userId);
            statement.setString(2, request.displayName());
            statement.setString(3, request.protocolType());
            statement.setString(4, request.baseUrl());
            statement.setString(5, request.defaultModel());
            return statement;
        }, keys);
        if (keys.getKey() == null) throw new IllegalStateException("创建模型配置失败：未返回主键");
        return keys.getKey().longValue();
    }

    public int update(long userId, long id, AiProviderProfileRequest request) {
        return jdbcTemplate.update("""
                UPDATE ai_provider_profiles
                SET display_name = ?, protocol_type = ?, base_url = ?, default_model = ?, updated_at = CURRENT_TIMESTAMP
                WHERE user_id = ? AND id = ?
                """, request.displayName(), request.protocolType(), request.baseUrl(), request.defaultModel(), userId, id);
    }

    public int delete(long userId, long id) {
        return jdbcTemplate.update("DELETE FROM ai_provider_profiles WHERE user_id = ? AND id = ?", userId, id);
    }

    public void clearDefault(long userId) {
        jdbcTemplate.update("UPDATE ai_provider_profiles SET is_default = FALSE, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?",
                userId);
    }

    public int setDefault(long userId, long id) {
        return jdbcTemplate.update("""
                UPDATE ai_provider_profiles SET is_default = TRUE, updated_at = CURRENT_TIMESTAMP
                WHERE user_id = ? AND id = ?
                """, userId, id);
    }

    public void recordTest(long userId, long id, boolean success, String message) {
        jdbcTemplate.update("""
                UPDATE ai_provider_profiles
                SET last_tested_at = ?, last_test_status = ?, last_test_message = ?, updated_at = CURRENT_TIMESTAMP
                WHERE user_id = ? AND id = ?
                """, LocalDateTime.now(), success ? "success" : "failed", message, userId, id);
    }

    private RowMapper<AiProviderProfile> mapper() {
        return (rs, rowNum) -> new AiProviderProfile(
                rs.getLong("id"), rs.getLong("user_id"), rs.getString("display_name"),
                rs.getString("protocol_type"), rs.getString("base_url"), rs.getString("default_model"),
                rs.getBoolean("is_default"),
                rs.getTimestamp("last_tested_at") == null ? null : rs.getTimestamp("last_tested_at").toLocalDateTime(),
                rs.getString("last_test_status"), rs.getString("last_test_message"),
                rs.getTimestamp("created_at").toLocalDateTime(), rs.getTimestamp("updated_at").toLocalDateTime());
    }
}
