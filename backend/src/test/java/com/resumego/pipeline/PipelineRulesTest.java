package com.resumego.pipeline;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PipelineRulesTest {

    private final PipelineRules rules = new PipelineRules();

    @Test
    void acceptsOutcomeOnlyForClosedPipeline() {
        assertThatCode(() -> rules.validateLifecycle(PipelineLifecycle.CLOSED, PipelineOutcome.OFFER))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> rules.validateLifecycle(PipelineLifecycle.ACTIVE, PipelineOutcome.OFFER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("结束结果");
        assertThatThrownBy(() -> rules.validateLifecycle(PipelineLifecycle.CLOSED, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("结束结果");
    }

    @Test
    void rejectsSameStageAndInactivePipelineTransitions() {
        assertThatThrownBy(() -> rules.validateStageTransition(11L, 11L, PipelineLifecycle.ACTIVE))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("当前阶段");
        assertThatThrownBy(() -> rules.validateStageTransition(11L, 12L, PipelineLifecycle.ARCHIVED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("归档");
        assertThatThrownBy(() -> rules.validateStageTransition(11L, 12L, PipelineLifecycle.CLOSED))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("结束");
        assertThatCode(() -> rules.validateStageTransition(11L, 12L, PipelineLifecycle.ACTIVE))
                .doesNotThrowAnyException();
    }
}
