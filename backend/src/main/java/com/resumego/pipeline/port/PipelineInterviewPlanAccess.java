package com.resumego.pipeline.port;

@FunctionalInterface
public interface PipelineInterviewPlanAccess {
    boolean existsForUser(long userId, long planId);
}
