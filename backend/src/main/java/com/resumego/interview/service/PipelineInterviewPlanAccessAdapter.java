package com.resumego.interview.service;

import com.resumego.interview.entity.InterviewPlan;
import com.resumego.interview.mapper.InterviewPlanMapper;
import com.resumego.pipeline.port.PipelineInterviewPlanAccess;
import org.springframework.stereotype.Component;

@Component
public class PipelineInterviewPlanAccessAdapter implements PipelineInterviewPlanAccess {

    private final InterviewPlanMapper mapper;

    public PipelineInterviewPlanAccessAdapter(InterviewPlanMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public boolean existsForUser(long userId, long planId) {
        InterviewPlan plan = mapper.selectById(planId);
        return plan != null && plan.getUserId() != null && plan.getUserId() == userId
                && plan.getDeletedAt() == null;
    }
}
