package com.resumego.interview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.resumego.interview.entity.InterviewSession;
import org.apache.ibatis.annotations.Mapper;

/**
 * interview_sessions 表 MyBatis-Plus Mapper。
 */
@Mapper
public interface InterviewSessionMapper extends BaseMapper<InterviewSession> {
}