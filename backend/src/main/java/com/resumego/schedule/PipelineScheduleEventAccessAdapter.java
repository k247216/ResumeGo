package com.resumego.schedule;

import com.resumego.pipeline.port.PipelineScheduleEventAccess;
import org.springframework.stereotype.Component;

@Component
public class PipelineScheduleEventAccessAdapter implements PipelineScheduleEventAccess {

    private final ScheduleEventRepository repository;

    public PipelineScheduleEventAccessAdapter(ScheduleEventRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean existsForUser(long userId, long eventId) {
        return repository.findById(userId, eventId).isPresent();
    }
}
