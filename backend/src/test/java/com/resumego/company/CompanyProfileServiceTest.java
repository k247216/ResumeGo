package com.resumego.company;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("CompanyProfileService 测试")
class CompanyProfileServiceTest {

    private JdbcTemplate jdbcTemplate;
    private CompanyProfileService companyProfileService;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        companyProfileService = new CompanyProfileService(jdbcTemplate, new ObjectMapper());
    }

    @Test
    @DisplayName("正常匹配公司名时从数据库行映射并返回 Profile")
    void shouldReturnProfileWhenCompanyNameMatched() throws Exception {
        mockDatabaseRow("字节跳动",
                "[\"experience_based\"]",
                "[\"业务结果\",\"快速迭代\"]",
                "[\"项目深挖\"]",
                "[\"补充量化结果\"]",
                Date.valueOf("2026-07-16"));

        Map<String, Object> result = companyProfileService.findEnabledProfileByCompanyName("字节跳动");

        assertThat(result).containsEntry("companyName", "字节跳动");
        assertThat(result.get("sourceType")).isEqualTo(List.of("experience_based"));
        assertThat(result.get("preferenceTags")).isEqualTo(List.of("业务结果", "快速迭代"));
        assertThat(result.get("interviewFocus")).isEqualTo(List.of("项目深挖"));
        assertThat(result.get("resumeAdviceRules")).isEqualTo(List.of("补充量化结果"));
        assertThat(result).containsEntry("lastVerifiedAt", "2026-07-16");
        verify(jdbcTemplate).query(anyString(), any(RowMapper.class), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("null 或空字符串不查询数据库并返回空 Map")
    void shouldReturnEmptyMapWhenCompanyNameBlank() {
        assertThat(companyProfileService.findEnabledProfileByCompanyName(null)).isEmpty();
        assertThat(companyProfileService.findEnabledProfileByCompanyName("  ")).isEmpty();

        verify(jdbcTemplate, never()).query(anyString(), any(RowMapper.class), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("常见公司别名会归一化后参与模糊匹配")
    void shouldUseNormalizedAliasForFuzzyMatch() {
        Map<String, Object> profile = profile("字节跳动");
        doAnswer(invocation -> {
            assertThat(Arrays.asList(invocation.getArguments())).contains("bytedance");
            return List.of(profile);
        }).when(jdbcTemplate).query(anyString(), any(RowMapper.class), any(), any(), any(), any(), any());

        Map<String, Object> result = companyProfileService.findEnabledProfileByCompanyName("北京字节跳动科技有限公司");

        assertThat(result).containsEntry("companyName", "字节跳动");
    }

    @ParameterizedTest
    @CsvSource({
            "腾讯科技,tencent",
            "阿里云,alibaba",
            "拼多多,pinduoduo",
            "哔哩哔哩,bilibili",
            "美团点评,meituan",
            "JD.com,jd",
            "百度,baidu",
            "快手,kuaishou",
            "Huawei,huawei"
    })
    @DisplayName("主流公司别名均会转换为 normalized_name")
    void shouldNormalizeMainstreamCompanyAliases(String input, String normalized) {
        doAnswer(invocation -> {
            assertThat(Arrays.asList(invocation.getArguments())).contains(normalized);
            return List.of(profile(input));
        }).when(jdbcTemplate).query(anyString(), any(RowMapper.class), any(), any(), any(), any(), any());

        Map<String, Object> result = companyProfileService.findEnabledProfileByCompanyName(input);

        assertThat(result).containsEntry("companyName", input);
    }

    @Test
    @DisplayName("数据库未命中时返回空 Map")
    void shouldReturnEmptyMapWhenNoProfileMatched() {
        whenQuery().thenReturn(List.of());

        Map<String, Object> result = companyProfileService.findEnabledProfileByCompanyName("未知公司");

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Profile JSON 字段异常时降级为空数组")
    void shouldFallbackToEmptyListWhenJsonFieldInvalid() throws Exception {
        mockDatabaseRow("异常公司",
                "not-json",
                "",
                null,
                "[\"补充证据\"]",
                null);

        Map<String, Object> result = companyProfileService.findEnabledProfileByCompanyName("异常公司");

        assertThat(result.get("sourceType")).isEqualTo(List.of());
        assertThat(result.get("preferenceTags")).isEqualTo(List.of());
        assertThat(result.get("interviewFocus")).isEqualTo(List.of());
        assertThat(result.get("resumeAdviceRules")).isEqualTo(List.of("补充证据"));
        assertThat(result.get("lastVerifiedAt")).isNull();
    }

    @Test
    @DisplayName("数据库异常时返回空 Map")
    void shouldReturnEmptyMapWhenDatabaseUnavailable() {
        whenQuery().thenThrow(new BadSqlGrammarException("select", "SELECT company_profiles", new SQLException("boom")));

        Map<String, Object> result = companyProfileService.findEnabledProfileByCompanyName("腾讯");

        assertThat(result).isEmpty();
    }

    private org.mockito.stubbing.OngoingStubbing<List<Map<String, Object>>> whenQuery() {
        return when(jdbcTemplate.query(anyString(), any(RowMapper.class), any(), any(), any(), any(), any()));
    }

    private Map<String, Object> profile(String companyName) {
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("companyName", companyName);
        profile.put("sourceType", List.of("experience_based"));
        profile.put("sourceNote", "经验型偏好，仅供演示");
        profile.put("preferenceTags", List.of("业务结果", "快速迭代"));
        profile.put("writingStyle", "突出动作和结果指标");
        profile.put("interviewFocus", List.of("项目深挖"));
        profile.put("resumeAdviceRules", List.of("补充量化结果"));
        profile.put("confidenceLevel", "medium");
        profile.put("lastVerifiedAt", "2026-07-16");
        return profile;
    }

    @SuppressWarnings("unchecked")
    private void mockDatabaseRow(String companyName,
                                 String sourceTypeJson,
                                 String preferenceTagsJson,
                                 String interviewFocusJson,
                                 String resumeAdviceRulesJson,
                                 Date lastVerifiedAt) throws Exception {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString("company_name")).thenReturn(companyName);
        when(rs.getString("source_type")).thenReturn(sourceTypeJson);
        when(rs.getString("source_note")).thenReturn("经验型偏好，仅供演示");
        when(rs.getString("preference_tags")).thenReturn(preferenceTagsJson);
        when(rs.getString("writing_style")).thenReturn("突出动作和结果指标");
        when(rs.getString("interview_focus")).thenReturn(interviewFocusJson);
        when(rs.getString("resume_advice_rules")).thenReturn(resumeAdviceRulesJson);
        when(rs.getString("confidence_level")).thenReturn("medium");
        when(rs.getDate("last_verified_at")).thenReturn(lastVerifiedAt);

        doAnswer(invocation -> {
            RowMapper<Map<String, Object>> mapper = invocation.getArgument(1);
            return List.of(mapper.mapRow(rs, 0));
        }).when(jdbcTemplate).query(anyString(), any(RowMapper.class), any(), any(), any(), any(), any());
    }
}
