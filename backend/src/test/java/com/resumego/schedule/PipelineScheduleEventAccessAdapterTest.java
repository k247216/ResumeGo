package com.resumego.schedule;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PipelineScheduleEventAccessAdapterTest {

    @Test
    void exposesOnlyAnExistingEventOwnedByTheRequestedUser() {
        ScheduleEventRepository repository = mock(ScheduleEventRepository.class);
        ScheduleEvent event = new ScheduleEvent(100L, 1L, "技术面", "interview",
                LocalDateTime.now(), null, null, null, LocalDateTime.now(), LocalDateTime.now());
        when(repository.findById(1L, 100L)).thenReturn(Optional.of(event));
        when(repository.findById(1L, 200L)).thenReturn(Optional.empty());
        var adapter = new PipelineScheduleEventAccessAdapter(repository);

        assertThat(adapter.existsForUser(1L, 100L)).isTrue();
        assertThat(adapter.existsForUser(1L, 200L)).isFalse();
    }
}
