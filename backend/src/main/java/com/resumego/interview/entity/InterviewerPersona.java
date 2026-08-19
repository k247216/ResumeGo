package com.resumego.interview.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 面试官人设实体，映射 interview_personas 表。
 */
@Getter
@Setter
@TableName("interview_personas")
public class InterviewerPersona {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("name")
    private String name;

    @TableField("title")
    private String title;

    @TableField("style")
    private String style;

    @TableField("avatar")
    private String avatar;

    @TableField("type")
    private String type;

    @TableField("user_id")
    private Long userId;

    @TableField("sort_order")
    private Integer sortOrder;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;
}