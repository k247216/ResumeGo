package com.resumego.interview.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InterviewExperienceParserTest {

    private final InterviewExperienceParser parser = new InterviewExperienceParser();

    @Test
    void extractsExplicitMetadataAndNumberedQuestionsWithoutInventingText() {
        var parsed = parser.parse("腾讯面经", """
                ---
                company: 腾讯
                role: Java 后端实习
                icon: tencent
                ---
                1. 讲讲 Redis 缓存一致性
                2. 你如何排查慢查询？
                """);

        assertThat(parsed.companyName()).isEqualTo("腾讯");
        assertThat(parsed.targetRole()).isEqualTo("Java 后端实习");
        assertThat(parsed.companyIconKey()).isEqualTo("tencent");
        assertThat(parsed.questions()).containsExactly("讲讲 Redis 缓存一致性", "你如何排查慢查询？");
    }

    @Test
    void rejectsContentWithoutAnExplicitQuestionList() {
        assertThatThrownBy(() -> parser.parse("面经", "这是一段没有题目边界的说明"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("序号或列表");
    }
}
