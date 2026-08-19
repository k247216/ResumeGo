package com.resumego.interview.entity;


import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Getter;
import lombok.Setter;

/**
 * 面试会话实体，映射 interview_sessions 表。
 * status 字段由状态机控制，AI 不得直接写入。
 */
@Getter
@Setter
@TableName("interview_sessions")
public class InterviewSession {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("user_id")
    private Long userId;

    @TableField("resume_version_id")
    private Long resumeVersionId;

    @TableField("job_description_id")
    private Long jobDescriptionId;

    @TableField("status")
    private String status;

    @TableField("current_question_index")
    private Integer currentQuestionIndex;

    @TableField("total_questions")
    private Integer totalQuestions;

    @TableField("started_at")
    private LocalDateTime startedAt;

    @TableField("completed_at")
    private LocalDateTime completedAt;

    @TableField("summary_json")
    private String summaryJson;

    @TableField("persona_id")
    private Long personaId;

    @TableField("persona_name")
    private String personaName;

    @TableField("persona_title")
    private String personaTitle;

    @TableField("plan_id")
    private Long planId;

    @TableField("round_order")
    private Integer roundOrder;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
