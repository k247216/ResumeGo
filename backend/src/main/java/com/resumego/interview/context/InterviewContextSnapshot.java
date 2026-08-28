package com.resumego.interview.context;

import java.util.List;

/**
 * 面试开始上下文快照：创建时一次性写入，不可修改。
 * 只保存引用 ID、用户可见名称、版本号、来源类型、persona 顺序、题量与 prompt/schema 版本；
 * 不保存简历正文、知识正文、回答、API Key 或绝对路径。
 */
public record InterviewContextSnapshot(
        String contextContractVersion,
        String mode,
        Long jobProjectId,
        String jobProjectName,
        Long resumeVersionId,
        String resumeTitle,
        Integer resumeVersionNo,
        Long jobDescriptionId,
        List<Long> knowledgeDocumentIds,
        List<String> knowledgeDocumentTitles,
        Long questionSetId,
        String questionSetTitle,
        String questionSetSourceType,
        List<Long> personaIds,
        List<String> personaNames,
        Integer questionCount,
        List<String> focusTags,
        String difficulty,
        String questionStyle,
        String followUpIntensity,
        String reviewMode,
        String promptVersion,
        String outputSchemaVersion,
        /** 面经题集原始题目索引的用户排序；为空表示沿用题集顺序。 */
        List<Integer> questionOrder
) {
    /** 兼容当前调用方：历史快照没有面经题目排序。 */
    public InterviewContextSnapshot(
            String contextContractVersion,
            String mode,
            Long jobProjectId,
            String jobProjectName,
            Long resumeVersionId,
            String resumeTitle,
            Integer resumeVersionNo,
            Long jobDescriptionId,
            List<Long> knowledgeDocumentIds,
            List<String> knowledgeDocumentTitles,
            Long questionSetId,
            String questionSetTitle,
            String questionSetSourceType,
            List<Long> personaIds,
            List<String> personaNames,
            Integer questionCount,
            List<String> focusTags,
            String difficulty,
            String questionStyle,
            String followUpIntensity,
            String reviewMode,
            String promptVersion,
            String outputSchemaVersion
    ) {
        this(contextContractVersion, mode, jobProjectId, jobProjectName, resumeVersionId,
                resumeTitle, resumeVersionNo, jobDescriptionId, knowledgeDocumentIds,
                knowledgeDocumentTitles, questionSetId, questionSetTitle, questionSetSourceType,
                personaIds, personaNames, questionCount, focusTags, difficulty, questionStyle,
                followUpIntensity, reviewMode, promptVersion, outputSchemaVersion, null);
    }

    /** 兼容历史快照构造：旧数据没有提问风格。 */
    public InterviewContextSnapshot(
            String contextContractVersion,
            String mode,
            Long jobProjectId,
            String jobProjectName,
            Long resumeVersionId,
            String resumeTitle,
            Integer resumeVersionNo,
            Long jobDescriptionId,
            List<Long> knowledgeDocumentIds,
            List<String> knowledgeDocumentTitles,
            Long questionSetId,
            String questionSetTitle,
            String questionSetSourceType,
            List<Long> personaIds,
            List<String> personaNames,
            Integer questionCount,
            List<String> focusTags,
            String difficulty,
            String followUpIntensity,
            String promptVersion,
            String outputSchemaVersion
    ) {
        this(contextContractVersion, mode, jobProjectId, jobProjectName, resumeVersionId,
                resumeTitle, resumeVersionNo, jobDescriptionId, knowledgeDocumentIds,
                knowledgeDocumentTitles, questionSetId, questionSetTitle, questionSetSourceType,
                personaIds, personaNames, questionCount, focusTags, difficulty, null,
                followUpIntensity, null, promptVersion, outputSchemaVersion, null);
    }

    public static final String CONTRACT_VERSION = "1";
    public static final String PROMPT_VERSION = "v1";
    public static final String OUTPUT_SCHEMA_VERSION = "v1";
}
