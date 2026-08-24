package com.resumego.schedule;

import com.resumego.schedule.dto.CreateScheduleEventRequest;
import com.resumego.schedule.dto.UpdateScheduleEventRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ScheduleEventServiceTest {

    private ScheduleEventRepository repository;
    private ScheduleEventService service;
    private ScheduleEvent event;

    @BeforeEach
    void setUp() {
        repository = mock(ScheduleEventRepository.class);
        service = new ScheduleEventService(repository);
        event = new ScheduleEvent(7L, 1L, "腾讯技术面", "interview",
                LocalDateTime.of(2026, 8, 25, 14, 0), null, "第一轮", 10L, 100L,
                LocalDateTime.now(), LocalDateTime.now());
        when(repository.findById(1L, 7L)).thenReturn(Optional.of(event));
    }

    @Test
    void createsWithNormalizedTitleAndOwnedJob() {
        when(repository.ownsJobDescription(1L, 10L)).thenReturn(true);
        when(repository.ownsJobProject(1L, 100L)).thenReturn(true);
        when(repository.findJobDescriptionIdForProject(1L, 100L)).thenReturn(10L);
        when(repository.create(eq(1L), eq("腾讯技术面"), eq("interview"),
                any(LocalDateTime.class), isNull(), eq("第一轮"), eq(10L), eq(100L))).thenReturn(7L);

        var result = service.create(new CreateScheduleEventRequest("  腾讯技术面  ", "interview",
                LocalDateTime.of(2026, 8, 25, 14, 0), null, "第一轮", 10L, 100L));

        assertThat(result.id()).isEqualTo(7L);
        assertThat(result.jobProjectId()).isEqualTo(100L);
        verify(repository).create(eq(1L), eq("腾讯技术面"), eq("interview"),
                any(LocalDateTime.class), isNull(), eq("第一轮"), eq(10L), eq(100L));
    }

    @Test
    void derivesCompatibilityJobFromProjectWhenClientOmitsIt() {
        when(repository.ownsJobProject(1L, 100L)).thenReturn(true);
        when(repository.findJobDescriptionIdForProject(1L, 100L)).thenReturn(10L);
        when(repository.create(eq(1L), eq("腾讯技术面"), eq("interview"),
                any(LocalDateTime.class), isNull(), isNull(), eq(10L), eq(100L))).thenReturn(7L);

        service.create(new CreateScheduleEventRequest("腾讯技术面", "interview",
                LocalDateTime.of(2026, 8, 25, 14, 0), null, null, null, 100L));

        verify(repository).create(eq(1L), eq("腾讯技术面"), eq("interview"),
                any(LocalDateTime.class), isNull(), isNull(), eq(10L), eq(100L));
    }

    @Test
    void rejectsMismatchedJobAndProjectAssociation() {
        when(repository.ownsJobProject(1L, 100L)).thenReturn(true);
        when(repository.findJobDescriptionIdForProject(1L, 100L)).thenReturn(10L);

        assertThatThrownBy(() -> service.create(new CreateScheduleEventRequest("日程", "interview",
                LocalDateTime.now(), null, null, 11L, 100L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("岗位与求职计划不一致");

        verify(repository, never()).create(anyLong(), anyString(), anyString(), any(), any(), any(), any(), any());
    }

    @Test
    void rejectsProjectOutsideCurrentUser() {
        when(repository.ownsJobProject(1L, 200L)).thenReturn(false);

        assertThatThrownBy(() -> service.create(new CreateScheduleEventRequest("日程", "interview",
                LocalDateTime.now(), null, null, null, 200L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("求职计划不可用");
    }

    @Test
    void rejectsUnknownEventType() {
        assertThatThrownBy(() -> service.create(new CreateScheduleEventRequest("日程", "phone",
                LocalDateTime.now(), null, null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("日程类型");
        verify(repository, never()).create(anyLong(), anyString(), anyString(), any(), any(), any(), any(), any());
    }

    @Test
    void rejectsEndBeforeStart() {
        assertThatThrownBy(() -> service.create(new CreateScheduleEventRequest("日程", "interview",
                LocalDateTime.of(2026, 8, 25, 14, 0), LocalDateTime.of(2026, 8, 25, 13, 0), null, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("结束时间");
    }

    @Test
    void rejectsJobOutsideCurrentUser() {
        when(repository.ownsJobDescription(1L, 20L)).thenReturn(false);

        assertThatThrownBy(() -> service.create(new CreateScheduleEventRequest("日程", "interview",
                LocalDateTime.now(), null, null, 20L, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不可用");
    }

    @Test
    void updatesAndDeletesAreExplicit() {
        when(repository.ownsJobDescription(1L, 10L)).thenReturn(true);
        when(repository.update(eq(1L), eq(7L), eq("腾讯技术面"), eq("interview"),
                any(LocalDateTime.class), isNull(), isNull(), isNull(), isNull())).thenReturn(1);
        when(repository.softDelete(1L, 7L)).thenReturn(1);

        service.update(7L, new UpdateScheduleEventRequest("腾讯技术面", "interview",
                LocalDateTime.of(2026, 8, 25, 14, 0), null, null, null, null));
        assertThat(service.delete(7L)).isTrue();

        verify(repository).update(eq(1L), eq(7L), eq("腾讯技术面"), eq("interview"),
                any(LocalDateTime.class), isNull(), isNull(), isNull(), isNull());
        verify(repository).softDelete(1L, 7L);
    }

    @Test
    void missingEventReportsNotFound() {
        when(repository.findById(1L, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(99L))
                .isInstanceOf(java.util.NoSuchElementException.class)
                .hasMessageContaining("日程不存在");
    }
}
