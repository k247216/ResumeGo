package com.resumego.project;

import com.resumego.project.dto.CreateJobProjectRequest;
import com.resumego.project.dto.UpdateJobProjectLinksRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class JobProjectServiceTest {

    private JobProjectRepository repository;
    private JobProjectService service;
    private JobProject project;

    @BeforeEach
    void setUp() {
        repository = mock(JobProjectRepository.class);
        service = new JobProjectService(repository);
        project = new JobProject(7L, 1L, "Java 实习", "active", "applied", 10L, 31L,
                null, null, null, null, null, null, LocalDateTime.now(), LocalDateTime.now());
        when(repository.findById(1L, 7L)).thenReturn(Optional.of(project));
    }

    @Test
    void createsWithNormalizedNameAndOwnedLinks() {
        when(repository.ownsJobDescription(1L, 10L)).thenReturn(true);
        when(repository.ownsResumeVersion(1L, 31L)).thenReturn(true);
        when(repository.create(1L, "Java 实习", 10L, 31L)).thenReturn(7L);

        var result = service.create(new CreateJobProjectRequest("  Java 实习  ", 10L, 31L));

        assertThat(result.id()).isEqualTo(7L);
        verify(repository).create(1L, "Java 实习", 10L, 31L);
    }

    @Test
    void rejectsLinksOutsideCurrentUser() {
        when(repository.ownsJobDescription(1L, 20L)).thenReturn(false);

        assertThatThrownBy(() -> service.create(new CreateJobProjectRequest("项目", 20L, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不可用");
        verify(repository, never()).create(anyLong(), anyString(), any(), any());
    }

    @Test
    void updatesLinksOnlyAfterOwnershipValidation() {
        when(repository.ownsResumeVersion(1L, 31L)).thenReturn(true);
        when(repository.updateLinks(1L, 7L, null, 31L)).thenReturn(1);

        service.updateLinks(7L, new UpdateJobProjectLinksRequest(null, 31L));

        verify(repository).updateLinks(1L, 7L, null, 31L);
    }

    @Test
    void updatesStageOnlyWithKnownValue() {
        when(repository.updateStage(1L, 7L, "interview")).thenReturn(1);

        service.updateStage(7L, new com.resumego.project.dto.UpdateJobProjectStageRequest(" interview "));

        verify(repository).updateStage(1L, 7L, "interview");

        assertThatThrownBy(() -> service.updateStage(7L, new com.resumego.project.dto.UpdateJobProjectStageRequest("hired")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("未知的求职阶段");
        verify(repository, never()).updateStage(1L, 7L, "hired");
    }

    @Test
    void updatesApplicationInfoAndNormalizesBlanks() {
        when(repository.updateApplicationInfo(1L, 7L, "互联网", "后端开发", "深圳", null)).thenReturn(1);

        service.updateApplicationInfo(7L, new com.resumego.project.dto.UpdateJobProjectApplicationRequest(" 互联网 ", " 后端开发 ", " 深圳 ", "   "));

        verify(repository).updateApplicationInfo(1L, 7L, "互联网", "后端开发", "深圳", null);
    }

    @Test
    void recordsStageEventOnUpdateAndListsHistory() {
        when(repository.updateStage(1L, 7L, "exam")).thenReturn(1);
        var now = LocalDateTime.now();
        when(repository.findStageEvents(1L, 7L)).thenReturn(List.of(
                new JobProjectRepository.StageEvent(2L, "exam", now),
                new JobProjectRepository.StageEvent(1L, "applied", now.minusDays(3))
        ));

        service.updateStage(7L, new com.resumego.project.dto.UpdateJobProjectStageRequest("exam"));
        var events = service.listStageEvents(7L);

        verify(repository).insertStageEvent(1L, 7L, "exam");
        assertThat(events).hasSize(2);
        assertThat(events.get(0).stage()).isEqualTo("exam");
    }

    @Test
    void rejectsBackwardStageMoves() {
        when(repository.findById(1L, 7L)).thenReturn(Optional.of(
                new JobProject(7L, 1L, "Java 实习", "active", "interview", 10L, 31L,
                        null, null, null, null, null, null, LocalDateTime.now(), LocalDateTime.now())));

        assertThatThrownBy(() -> service.updateStage(7L, new com.resumego.project.dto.UpdateJobProjectStageRequest("applied")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("不能回退");
        verify(repository, never()).updateStage(anyLong(), anyLong(), anyString());
    }

    @Test
    void locksStageOnceTerminal() {
        when(repository.findById(1L, 7L)).thenReturn(Optional.of(
                new JobProject(7L, 1L, "Java 实习", "active", "rejected", 10L, 31L,
                        null, null, null, null, null, null, LocalDateTime.now(), LocalDateTime.now())));

        assertThatThrownBy(() -> service.updateStage(7L, new com.resumego.project.dto.UpdateJobProjectStageRequest("exam")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("已锁定");
        verify(repository, never()).updateStage(anyLong(), anyLong(), anyString());
    }

    @Test
    void archiveRestoreAndDeleteAreExplicit() {
        when(repository.archive(1L, 7L)).thenReturn(1);
        when(repository.restore(1L, 7L)).thenReturn(1);
        when(repository.softDelete(1L, 7L)).thenReturn(1);

        service.archive(7L);
        when(repository.findById(1L, 7L)).thenReturn(Optional.of(
                new JobProject(7L, 1L, "Java 实习", "archived", "closed", 10L, 31L,
                        LocalDateTime.now(), null, null, null, null, null, LocalDateTime.now(), LocalDateTime.now())));
        service.restore(7L);
        assertThat(service.delete(7L)).isTrue();

        verify(repository).archive(1L, 7L);
        verify(repository).restore(1L, 7L);
        verify(repository).softDelete(1L, 7L);
    }
}
