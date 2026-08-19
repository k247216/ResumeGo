package com.resumego.interview.entity;


import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import lombok.Getter;
import lombok.Setter;

/**
 * 面试评价实体，映射 interview_evaluations 表。
 * AI 评价不代表录用概率，评价结果必须通过结构化校验后才能保存。
 */
@Getter
@Setter
@TableName("interview_evaluations")
public class InterviewEvaluation {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("session_id")
    private Long sessionId;

    @TableField("question_id")
    private Long questionId;

    @TableField("answer_id")
    private Long answerId;

    @TableField("score_json")
    private String scoreJson;

    @TableField("strengths_json")
    private String strengthsJson;

    @TableField("weaknesses_json")
    private String weaknessesJson;

    @TableField("suggestions_json")
    private String suggestionsJson;

    @TableField("reference_answer_json")
    private String referenceAnswerJson;

    @TableField("evaluation_ai_invocation_id")
    private Long evaluationAiInvocationId;

    @TableField("created_at")
    private LocalDateTime createdAt;
}