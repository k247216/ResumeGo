package com.resumego.pipeline;

import org.springframework.stereotype.Component;

@Component
public final class PipelineRules {

    public void validateLifecycle(PipelineLifecycle lifecycle, PipelineOutcome outcome) {
        if (lifecycle == PipelineLifecycle.CLOSED && outcome == null) {
            throw new IllegalArgumentException("结束的求职管线必须记录结束结果");
        }
        if (lifecycle != PipelineLifecycle.CLOSED && outcome != null) {
            throw new IllegalArgumentException("只有结束的求职管线可以记录结束结果");
        }
    }

    public void validateStageTransition(long currentStageId, long targetStageId,
                                        PipelineLifecycle lifecycle) {
        if (currentStageId == targetStageId) {
            throw new IllegalArgumentException("目标不能是当前阶段");
        }
        if (lifecycle == PipelineLifecycle.ARCHIVED) {
            throw new IllegalStateException("已归档的求职管线不能切换阶段");
        }
        if (lifecycle == PipelineLifecycle.CLOSED) {
            throw new IllegalStateException("已结束的求职管线不能切换阶段");
        }
    }
}
