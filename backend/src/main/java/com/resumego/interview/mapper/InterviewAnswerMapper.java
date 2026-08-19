package com.resumego.interview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.resumego.interview.entity.InterviewAnswer;
import org.apache.ibatis.annotations.Mapper;

/**
 * interview_answers 表 MyBatis-Plus Mapper。
 */
@Mapper
public interface InterviewAnswerMapper extends BaseMapper<InterviewAnswer> {
}