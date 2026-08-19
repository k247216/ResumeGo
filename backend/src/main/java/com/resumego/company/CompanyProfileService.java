package com.resumego.company;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class CompanyProfileService {

    private static final Logger log = LoggerFactory.getLogger(CompanyProfileService.class);

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public CompanyProfileService(JdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> findEnabledProfileByCompanyName(String companyName) {
        if (companyName == null || companyName.isBlank()) {
            return Map.of();
        }
        String normalized = normalizeCompanyName(companyName);
        try {
            List<Map<String, Object>> rows = jdbcTemplate.query("""
                    SELECT company_name, source_type, source_note, preference_tags,
                           writing_style, interview_focus, resume_advice_rules,
                           confidence_level, last_verified_at
                    FROM company_profiles
                    WHERE enabled = TRUE
                      AND (
                        company_name = ?
                        OR ? LIKE CONCAT('%', company_name, '%')
                        OR company_name LIKE CONCAT('%', ?, '%')
                        OR normalized_name = ?
                      )
                    ORDER BY
                      CASE WHEN company_name = ? THEN 0 ELSE 1 END,
                      LENGTH(company_name) DESC
                    LIMIT 1
                    """,
                    (rs, rowNum) -> {
                        Map<String, Object> profile = new LinkedHashMap<>();
                        profile.put("companyName", rs.getString("company_name"));
                        profile.put("sourceType", parseJsonList(rs.getString("source_type")));
                        profile.put("sourceNote", rs.getString("source_note"));
                        profile.put("preferenceTags", parseJsonList(rs.getString("preference_tags")));
                        profile.put("writingStyle", rs.getString("writing_style"));
                        profile.put("interviewFocus", parseJsonList(rs.getString("interview_focus")));
                        profile.put("resumeAdviceRules", parseJsonList(rs.getString("resume_advice_rules")));
                        profile.put("confidenceLevel", rs.getString("confidence_level"));
                        profile.put("lastVerifiedAt", rs.getDate("last_verified_at") != null
                                ? rs.getDate("last_verified_at").toString() : null);
                        return profile;
                    },
                    companyName,
                    companyName,
                    companyName,
                    normalized,
                    companyName
            );
            return rows == null || rows.isEmpty() ? Map.of() : rows.getFirst();
        } catch (DataAccessException e) {
            log.debug("公司偏好 Profile 未命中或不可用: companyName={}", companyName);
            return Map.of();
        }
    }

    @SuppressWarnings("unchecked")
    private List<Object> parseJsonList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            Object value = objectMapper.readValue(json, Object.class);
            if (value instanceof List<?> list) {
                return (List<Object>) list;
            }
        } catch (Exception e) {
            log.debug("公司偏好 JSON 数组解析失败");
        }
        return List.of();
    }

    private String normalizeCompanyName(String companyName) {
        String value = companyName == null ? "" : companyName.toLowerCase();
        if (value.contains("字节") || value.contains("bytedance") || value.contains("抖音")) return "bytedance";
        if (value.contains("腾讯") || value.contains("tencent")) return "tencent";
        if (value.contains("阿里") || value.contains("alibaba")) return "alibaba";
        if (value.contains("拼多多") || value.contains("pinduoduo")) return "pinduoduo";
        if (value.contains("哔哩") || value.contains("bilibili") || value.contains("b站")) return "bilibili";
        if (value.contains("美团") || value.contains("meituan")) return "meituan";
        if (value.contains("京东") || value.contains("jd.com") || value.contains("jingdong")) return "jd";
        if (value.contains("百度") || value.contains("baidu")) return "baidu";
        if (value.contains("快手") || value.contains("kuaishou")) return "kuaishou";
        if (value.contains("华为") || value.contains("huawei") || value.contains("hua wei")) return "huawei";
        return value.replaceAll("[^a-z0-9\\u4e00-\\u9fa5]", "");
    }
}
