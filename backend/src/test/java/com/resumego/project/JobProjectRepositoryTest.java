package com.resumego.project;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Import(JobProjectRepository.class)
@Sql(scripts = "/sql/job_projects_schema.sql")
class JobProjectRepositoryTest {

    @Autowired
    private JobProjectRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void scopesReadsToUserAndExcludesSoftDeletedProjects() {
        List<JobProject> projects = repository.findAll(1L);

        assertThat(projects).extracting(JobProject::id).containsExactly(100L, 101L);
        assertThat(repository.findById(1L, 100L)).isPresent();
        assertThat(repository.findById(1L, 102L)).isEmpty();
        assertThat(repository.findById(1L, 200L)).isEmpty();
    }

    @Test
    void createsAndUpdatesProjectLifecycle() {
        long id = repository.create(1L, "后端实习", 10L, 31L);

        assertThat(repository.findById(1L, id)).get()
                .extracting(JobProject::name, JobProject::status,
                        JobProject::jobDescriptionId, JobProject::resumeVersionId)
                .containsExactly("后端实习", "active", 10L, 31L);

        assertThat(repository.rename(1L, id, "Java 后端实习")).isOne();
        assertThat(repository.updateLinks(1L, id, null, null)).isOne();
        assertThat(repository.archive(1L, id)).isOne();
        assertThat(repository.findById(1L, id)).get()
                .extracting(JobProject::name, JobProject::status,
                        JobProject::jobDescriptionId, JobProject::resumeVersionId)
                .containsExactly("Java 后端实习", "archived", null, null);

        assertThat(repository.restore(1L, id)).isOne();
        assertThat(repository.findById(1L, id)).get()
                .extracting(JobProject::status, JobProject::archivedAt)
                .containsExactly("active", null);
    }

    @Test
    void validatesOwnershipAndPreservesLinkedAssetsAfterSoftDelete() {
        assertThat(repository.ownsJobDescription(1L, 10L)).isTrue();
        assertThat(repository.ownsJobDescription(1L, 20L)).isFalse();
        assertThat(repository.ownsResumeVersion(1L, 31L)).isTrue();
        assertThat(repository.ownsResumeVersion(1L, 41L)).isFalse();

        assertThat(repository.softDelete(1L, 100L)).isOne();
        assertThat(repository.findById(1L, 100L)).isEmpty();
        assertThat(count("job_descriptions", 10L)).isOne();
        assertThat(count("resume_versions", 31L)).isOne();
    }

    private long count(String table, long id) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE id = ?",
                Long.class,
                id
        );
        return count == null ? 0 : count;
    }
}
