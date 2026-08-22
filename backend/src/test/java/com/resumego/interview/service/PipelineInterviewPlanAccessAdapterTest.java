package com.resumego.interview.service;

import com.resumego.interview.entity.InterviewPlan;
import com.resumego.interview.mapper.InterviewPlanMapper;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PipelineInterviewPlanAccessAdapterTest {

    @Test
    void exposesOnlyAnActivePlanOwnedByTheRequestedUser() {
        InterviewPlanMapper mapper = mock(InterviewPlanMapper.class);
        InterviewPlan owned = plan(300L, 1L, null);
        InterviewPlan foreign = plan(400L, 2L, null);
        InterviewPlan hidden = plan(500L, 1L, LocalDateTime.now());
        when(mapper.selectById(300L)).thenReturn(owned);
        when(mapper.selectById(400L)).thenReturn(foreign);
        when(mapper.selectById(500L)).thenReturn(hidden);
        var adapter = new PipelineInterviewPlanAccessAdapter(mapper);

        assertThat(adapter.existsForUser(1L, 300L)).isTrue();
        assertThat(adapter.existsForUser(1L, 400L)).isFalse();
        assertThat(adapter.existsForUser(1L, 500L)).isFalse();
        assertThat(adapter.existsForUser(1L, 999L)).isFalse();
    }

    private InterviewPlan plan(long id, long userId, LocalDateTime deletedAt) {
        InterviewPlan plan = new InterviewPlan();
        plan.setId(id);
        plan.setUserId(userId);
        plan.setDeletedAt(deletedAt);
        return plan;
    }
}
