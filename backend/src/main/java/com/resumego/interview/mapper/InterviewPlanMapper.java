package com.resumego.interview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.resumego.interview.entity.InterviewPlan;
import org.apache.ibatis.annotations.Mapper;

/**
 * interview_plans 表 MyBatis-Plus Mapper。
 */
@Mapper
public interface InterviewPlanMapper extends BaseMapper<InterviewPlan> {
}
