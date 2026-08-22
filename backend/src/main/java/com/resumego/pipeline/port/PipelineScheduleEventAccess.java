package com.resumego.pipeline.port;

@FunctionalInterface
public interface PipelineScheduleEventAccess {
    boolean existsForUser(long userId, long eventId);
}
