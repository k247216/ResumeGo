package com.resumego.project;

import com.resumego.project.dto.CreateJobProjectRequest;
import com.resumego.project.dto.UpdateJobProjectLinksRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
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
        project = new JobProject(7L, 1L, "Java 实习", "active", 10L, 31L,
                null, LocalDateTime.now(), LocalDateTime.now());
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
    void archiveRestoreAndDeleteAreExplicit() {
        when(repository.archive(1L, 7L)).thenReturn(1);
        when(repository.restore(1L, 7L)).thenReturn(1);
        when(repository.softDelete(1L, 7L)).thenReturn(1);

        service.archive(7L);
        when(repository.findById(1L, 7L)).thenReturn(Optional.of(
                new JobProject(7L, 1L, "Java 实习", "archived", 10L, 31L,
                        LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now())));
        service.restore(7L);
        assertThat(service.delete(7L)).isTrue();

        verify(repository).archive(1L, 7L);
        verify(repository).restore(1L, 7L);
        verify(repository).softDelete(1L, 7L);
    }
}
