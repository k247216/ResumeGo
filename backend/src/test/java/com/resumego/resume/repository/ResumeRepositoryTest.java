package com.resumego.resume.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumego.resume.dto.ResumeVersionDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** 简历/版本持久化与 JSON 路径应用测试；不含评分或岗位排序规则。 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ResumeRepository 单元测试")
class ResumeRepositoryTest {
    @Mock private JdbcTemplate jdbcTemplate;
    private ResumeRepository repository;

    @BeforeEach
    void setUp() { repository = new ResumeRepository(jdbcTemplate, new ObjectMapper()); }

    @Test
    void shouldQueryResumeIdsAndBasicColumns() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyLong())).thenReturn(List.of(2L, 5L));
        when(jdbcTemplate.queryForObject(anyString(), org.mockito.ArgumentMatchers.eq(String.class), anyLong()))
                .thenReturn("后端求职简历");
        when(jdbcTemplate.queryForObject(org.mockito.ArgumentMatchers.contains("current_version_id"),
                org.mockito.ArgumentMatchers.eq(Long.class), anyLong())).thenReturn(12L);
        when(jdbcTemplate.queryForObject(org.mockito.ArgumentMatchers.contains("target_job_description_id"),
                org.mockito.ArgumentMatchers.eq(Long.class), anyLong())).thenReturn(30L);

        assertThat(repository.findIdsByUserId(1L)).containsExactly(2L, 5L);
        assertThat(repository.findTitleById(2L)).isEqualTo("后端求职简历");
        assertThat(repository.findCurrentVersionId(2L)).isEqualTo(12L);
        assertThat(repository.findTargetJobDescriptionIdById(2L)).isEqualTo(30L);
    }

    @Test
    void shouldInsertResumeAndVersionAndReturnGeneratedIds() {
        when(jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class)).thenReturn(9L, 10L);

        assertThat(repository.insertResume(1L, "新简历", 20L)).isEqualTo(9L);
        assertThat(repository.insertVersion(9L, null, 1, "{}", "创建", "user")).isEqualTo(10L);
        verify(jdbcTemplate).update(org.mockito.ArgumentMatchers.contains("INSERT INTO resumes"),
                org.mockito.ArgumentMatchers.eq(1L), org.mockito.ArgumentMatchers.eq("新简历"), org.mockito.ArgumentMatchers.eq(20L));
        verify(jdbcTemplate).update(org.mockito.ArgumentMatchers.contains("INSERT INTO resume_versions"),
                org.mockito.ArgumentMatchers.eq(9L), org.mockito.ArgumentMatchers.isNull(), org.mockito.ArgumentMatchers.eq(1),
                org.mockito.ArgumentMatchers.eq("{}"), org.mockito.ArgumentMatchers.eq("创建"), org.mockito.ArgumentMatchers.eq("user"));
    }

    @Test
    void shouldReturnZeroWhenDatabaseHasNoGeneratedId() {
        when(jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class)).thenReturn(null);

        assertThat(repository.insertResume(1L, "新简历", null)).isZero();
        assertThat(repository.insertVersion(1L, null, 1, "{}", null, "user")).isZero();
    }

    @Test
    void shouldUpdateCurrentAndTargetJobReferences() {
        repository.updateCurrentVersionId(1L, 2L);
        repository.updateTargetJobDescriptionId(1L, 3L);

        verify(jdbcTemplate).update(org.mockito.ArgumentMatchers.contains("current_version_id"),
                org.mockito.ArgumentMatchers.eq(2L), org.mockito.ArgumentMatchers.eq(1L));
        verify(jdbcTemplate).update(org.mockito.ArgumentMatchers.contains("target_job_description_id"),
                org.mockito.ArgumentMatchers.eq(3L), org.mockito.ArgumentMatchers.eq(1L));
    }

    @Test
    void shouldReturnMaxVersionOrZero() {
        when(jdbcTemplate.queryForObject(org.mockito.ArgumentMatchers.contains("MAX(version_no)"),
                org.mockito.ArgumentMatchers.eq(Integer.class), anyLong())).thenReturn(8);
        assertThat(repository.findMaxVersionNo(1L)).isEqualTo(8);
        when(jdbcTemplate.queryForObject(org.mockito.ArgumentMatchers.contains("MAX(version_no)"),
                org.mockito.ArgumentMatchers.eq(Integer.class), anyLong())).thenReturn(null);
        assertThat(repository.findMaxVersionNo(1L)).isZero();
    }

    @Test
    void shouldReturnVersionRowsOrNullWhenNotFound() {
        ResumeVersionDTO version = new ResumeVersionDTO(7L, 1L, null, 2,
                Map.of("summary", "文本"), "更新", "user", LocalDateTime.now());
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyLong())).thenReturn(List.of(version));
        assertThat(repository.findVersionById(7L)).isEqualTo(version);
        assertThat(repository.findVersionsByResumeId(1L)).containsExactly(version);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class), anyLong())).thenReturn(List.of());
        assertThat(repository.findVersionById(999L)).isNull();
    }

    @Test
    void shouldApplySuggestionToPlainNestedAndListFields() throws Exception {
        String content = """
                {"summary":"旧简介","projects":[{"description":"旧项目","highlights":["旧亮点","第二项"]}],"skills":["Java","MySQL"]}
                """;
        ObjectMapper mapper = new ObjectMapper();

        Map<?, ?> summary = mapper.readValue(repository.applySuggestion(content, "summary", "新简介"), Map.class);
        Map<?, ?> project = mapper.readValue(repository.applySuggestion(content, "projects[0].description", "新项目"), Map.class);
        Map<?, ?> list = mapper.readValue(repository.applySuggestion(content, "projects[0].highlights[1]", "新亮点"), Map.class);
        Map<?, ?> skills = mapper.readValue(repository.applySuggestion(content, "skills[1]", "Redis"), Map.class);

        assertThat(summary.get("summary")).isEqualTo("新简介");
        assertThat(project.toString()).contains("新项目");
        assertThat(list.toString()).contains("新亮点");
        assertThat(skills.toString()).contains("Java", "Redis");
    }

    @Test
    void shouldApplySuggestionToObjectAsDescription() throws Exception {
        String changed = repository.applySuggestion("{\"projects\":[{\"title\":\"职达\"}]}", "projects[0]", "补充项目说明");
        Map<?, ?> root = new ObjectMapper().readValue(changed, Map.class);
        Map<?, ?> project = (Map<?, ?>) ((List<?>) root.get("projects")).getFirst();

        assertThat(project.toString()).contains("description=补充项目说明");
    }

    @Test
    void shouldWrapInvalidJsonAsDomainException() {
        assertThatThrownBy(() -> repository.applySuggestion("not-json", "summary", "新简介"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("应用建议到简历内容失败");
    }
}
