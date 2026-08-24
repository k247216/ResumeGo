package com.resumego.schedule;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@Import(ScheduleEventRepository.class)
@Sql(scripts = "/sql/schedule_events_schema.sql")
class ScheduleEventRepositoryTest {

    @Autowired
    private ScheduleEventRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void scopesReadsToUserAndExcludesSoftDeletedEvents() {
        List<ScheduleEvent> events = repository.findAll(1L, null, null);

        assertThat(events).extracting(ScheduleEvent::id).containsExactly(100L, 101L);
        assertThat(repository.findById(1L, 100L)).isPresent();
        assertThat(repository.findById(1L, 102L)).isEmpty();
        assertThat(repository.findById(1L, 200L)).isEmpty();
    }

    @Test
    void filtersByTimeRange() {
        LocalDateTime from = LocalDateTime.of(2026, 8, 26, 0, 0);
        LocalDateTime to = LocalDateTime.of(2026, 8, 29, 0, 0);

        List<ScheduleEvent> events = repository.findAll(1L, from, to);

        assertThat(events).extracting(ScheduleEvent::id).containsExactly(101L);
    }

    @Test
    void createsAndUpdatesLifecycle() {
        long id = repository.create(1L, "线下二面", "interview",
                LocalDateTime.of(2026, 9, 1, 10, 0), null, "带简历", null, 100L);

        assertThat(repository.findById(1L, id)).get()
                .extracting(ScheduleEvent::title, ScheduleEvent::eventType, ScheduleEvent::notes, ScheduleEvent::jobProjectId)
                .containsExactly("线下二面", "interview", "带简历", 100L);

        assertThat(repository.update(1L, id, "线下二面（改期）", "interview",
                LocalDateTime.of(2026, 9, 2, 10, 0), null, null, null, 100L)).isOne();
        assertThat(repository.findById(1L, id)).get()
                .extracting(ScheduleEvent::title, ScheduleEvent::startTime)
                .containsExactly("线下二面（改期）", LocalDateTime.of(2026, 9, 2, 10, 0));

        assertThat(repository.softDelete(1L, id)).isOne();
        assertThat(repository.findById(1L, id)).isEmpty();
    }

    @Test
    void ownsJobDescriptionChecksUser() {
        assertThat(repository.ownsJobDescription(1L, 10L)).isTrue();
        assertThat(repository.ownsJobDescription(1L, 20L)).isFalse();
        assertThat(repository.findJobDescriptionIdForProject(1L, 100L)).isEqualTo(10L);
    }
}
