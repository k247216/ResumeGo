-- 面试官人设表
CREATE TABLE IF NOT EXISTS interview_personas (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    name VARCHAR(20) NOT NULL COMMENT '人设名称',
    title VARCHAR(50) NOT NULL COMMENT '职位头衔',
    style VARCHAR(200) NOT NULL COMMENT '风格描述',
    avatar VARCHAR(50) NOT NULL COMMENT '头像标识',
    type VARCHAR(20) NOT NULL DEFAULT 'preset' COMMENT '类型: preset/custom',
    user_id BIGINT UNSIGNED NULL COMMENT '创建者用户ID(自定义人设)',
    sort_order INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '排序',
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    KEY idx_interview_personas_type (type),
    KEY idx_interview_personas_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='面试官人设表';

-- 面试会话表增加人设关联
ALTER TABLE interview_sessions
    ADD COLUMN persona_id BIGINT UNSIGNED NULL COMMENT '面试官人设ID',
    ADD COLUMN persona_name VARCHAR(20) NULL COMMENT '面试官人设名称(冗余)',
    ADD COLUMN persona_title VARCHAR(50) NULL COMMENT '面试官人设职位(冗余)';