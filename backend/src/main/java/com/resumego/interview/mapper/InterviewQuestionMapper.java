package com.resumego.interview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.resumego.interview.entity.InterviewQuestion;
import org.apache.ibatis.annotations.Mapper;

/**
 * interview_questions 表 MyBatis-Plus Mapper。
 */
@Mapper
public interface InterviewQuestionMapper extends BaseMapper<InterviewQuestion> {
}