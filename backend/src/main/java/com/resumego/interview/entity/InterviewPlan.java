package com.resumego.interview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 多轮模拟面试计划。
 * 一个 plan 表示“一次面试”，其下多个 interview_sessions 表示不同面试官轮次。
 * 本实体不承载面试状态机转换逻辑。
 */
@Getter
@Setter
@TableName("interview_plans")
public class InterviewPlan {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    /** 训练模式（ROLE_BASED/KNOWLEDGE_TRAINING/EXPERIENCE_SIMULATION），创建后不可变 */
    @TableField("mode")
    private String mode;

    /** 开始上下文契约版本 */
    @TableField("context_contract_version")
    private String contextContractVersion;

    /** 不可变开始上下文快照 JSON（不含正文、API Key、绝对路径） */
    @TableField("start_context_snapshot_json")
    private String startContextSnapshotJson;

    @TableField("resume_version_id")
    private Long resumeVersionId;

    @TableField("job_description_id")
    private Long jobDescriptionId;

    @TableField("title")
    private String title;

    @TableField("question_count")
    private Integer questionCount;

    @TableField("persona_plan_json")
    private String personaPlanJson;

    @TableField("focus_tags_json")
    private String focusTagsJson;

    @TableField("supplement_text")
    private String supplementText;

    @TableField("summary_json")
    private String summaryJson;

    @TableField("summary_generated_at")
    private LocalDateTime summaryGeneratedAt;

    @TableField("deleted_at")
    private LocalDateTime deletedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
