package com.resumego.ai;

import com.resumego.job.dto.ParsedJobDescriptionDTO;

/**
 * JD 解析服务接口。
 * Sprint 1 使用 Stub 实现，后续 S1-08 替换为千问 Max 真实调用。
 */
public interface JdParseService {

    /**
     * 解析原始 JD 文本，抽取结构化岗位要求。
     *
     * @param rawText 用户粘贴的 JD 原文
     * @return 结构化的岗位要求
     */
    ParsedJobDescriptionDTO parse(String rawText);
}
