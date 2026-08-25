package com.resumego.interview.source;

import com.resumego.interview.QuestionSourceType;

/**
 * 题目草稿：来源引擎产出的最小题目单元。
 * sourceReference 指向可追溯来源（如题集条目、知识文档片段）；provenanceLabel 是用户可见来源标签。
 */
public record QuestionDraft(
        String text,
        String questionType,
        QuestionSourceType sourceType,
        String sourceReference,
        String provenanceLabel
) {
}
