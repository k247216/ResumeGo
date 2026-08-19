package com.resumego.matching.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.resumego.matching.entity.JobMatch;
import org.apache.ibatis.annotations.Mapper;

/**
 * job_matches 表 MyBatis-Plus Mapper。
 * 幂等查询方法由 MatchingPipelineService 通过 QueryWrapper 拼接。
 */
@Mapper
public interface JobMatchMapper extends BaseMapper<JobMatch> {
}
