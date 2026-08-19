package com.resumego.optimization;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * SuggestionValidation 安全校验规则单元测试。
 * 覆盖所有入参校验路径，确保非法输入被正确拦截。
 */
class SuggestionValidationTest {

    // ── requirePositive ──

    @Test
    @DisplayName("正数 ID 通过校验")
    void shouldPassRequirePositiveForValidId() {
        assertDoesNotThrow(() -> SuggestionValidation.requirePositive(1L, "testId"));
        assertDoesNotThrow(() -> SuggestionValidation.requirePositive(Long.MAX_VALUE, "testId"));
    }

    @ParameterizedTest
    @ValueSource(longs = {0L, -1L, -100L, Long.MIN_VALUE})
    @DisplayName("零或负数 ID 被拒绝")
    void shouldRejectRequirePositiveForNonPositiveId(long id) {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> SuggestionValidation.requirePositive(id, "testId"));
        assertThat(ex.getMessage()).contains("testId").contains("正整");
    }

    // ── validateSectionKey ──

    @ParameterizedTest
    @ValueSource(strings = {"projects[0]", "skills", "basicInfo.summary", "education[0]", "a1_b2[c3].d4"})
    @DisplayName("合法 sectionKey 通过校验")
    void shouldPassValidSectionKeys(String key) {
        assertDoesNotThrow(() -> SuggestionValidation.validateSectionKey(key));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "[0]projects", "projects[0]!", "projects[0]<script>"})
    @DisplayName("非法 sectionKey 被拒绝")
    void shouldRejectInvalidSectionKeys(String key) {
        assertThrows(IllegalArgumentException.class,
                () -> SuggestionValidation.validateSectionKey(key));
    }

    @Test
    @DisplayName("null sectionKey 被拒绝")
    void shouldRejectNullSectionKey() {
        assertThrows(IllegalArgumentException.class,
                () -> SuggestionValidation.validateSectionKey(null));
    }

    @Test
    @DisplayName("超长 sectionKey 被拒绝")
    void shouldRejectOverlyLongSectionKey() {
        String longKey = "a".repeat(SuggestionValidation.MAX_SECTION_KEY_LENGTH + 1);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> SuggestionValidation.validateSectionKey(longKey));
        assertThat(ex.getMessage()).contains("长度")
                .contains(String.valueOf(SuggestionValidation.MAX_SECTION_KEY_LENGTH));
    }

    // ── validateTextField ──

    @Test
    @DisplayName("合法文本字段通过校验")
    void shouldPassValidTextField() {
        assertDoesNotThrow(() -> SuggestionValidation.validateTextField("hello", "testField", 100));
        assertDoesNotThrow(() -> SuggestionValidation.validateTextField("a".repeat(100), "testField", 100));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("空白文本字段被拒绝")
    void shouldRejectBlankTextField(String text) {
        assertThrows(IllegalArgumentException.class,
                () -> SuggestionValidation.validateTextField(text, "testField", 100));
    }

    @Test
    @DisplayName("null 文本字段被拒绝")
    void shouldRejectNullTextField() {
        assertThrows(IllegalArgumentException.class,
                () -> SuggestionValidation.validateTextField(null, "testField", 100));
    }

    @Test
    @DisplayName("超长文本字段被拒绝")
    void shouldRejectOverlyLongTextField() {
        String longText = "a".repeat(101);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> SuggestionValidation.validateTextField(longText, "testField", 100));
        assertThat(ex.getMessage()).contains("testField").contains("长度").contains("100");
    }

    // ── validateConfidence ──

    @ParameterizedTest
    @ValueSource(strings = {"high", "medium", "low"})
    @DisplayName("合法 confidence 取值通过校验")
    void shouldPassValidConfidence(String value) {
        assertDoesNotThrow(() -> SuggestionValidation.validateConfidence(value));
    }

    @ParameterizedTest
    @ValueSource(strings = {"HIGH", "High", "unknown", "critical", ""})
    @DisplayName("非法 confidence 取值被拒绝")
    void shouldRejectInvalidConfidence(String value) {
        assertThrows(IllegalArgumentException.class,
                () -> SuggestionValidation.validateConfidence(value));
    }

    @Test
    @DisplayName("null confidence 被拒绝")
    void shouldRejectNullConfidence() {
        assertThrows(IllegalArgumentException.class,
                () -> SuggestionValidation.validateConfidence(null));
    }

    // ── validateSuggestionsCount ──

    @Test
    @DisplayName("合法数量通过校验")
    void shouldPassValidSuggestionsCount() {
        assertDoesNotThrow(() -> SuggestionValidation.validateSuggestionsCount(0));
        assertDoesNotThrow(() -> SuggestionValidation.validateSuggestionsCount(1));
        assertDoesNotThrow(() -> SuggestionValidation.validateSuggestionsCount(
                SuggestionValidation.MAX_SUGGESTIONS));
    }

    @Test
    @DisplayName("超出上限的数量被拒绝")
    void shouldRejectExcessiveSuggestionsCount() {
        int excessive = SuggestionValidation.MAX_SUGGESTIONS + 1;
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> SuggestionValidation.validateSuggestionsCount(excessive));
        assertThat(ex.getMessage()).contains(String.valueOf(SuggestionValidation.MAX_SUGGESTIONS));
    }

    // ── 常量验证 ──

    @Test
    @DisplayName("常量值在合理范围内")
    void shouldHaveReasonableConstants() {
        assertThat(SuggestionValidation.MAX_SUGGESTIONS).isBetween(1, 100);
        assertThat(SuggestionValidation.MAX_SECTION_KEY_LENGTH).isBetween(10, 500);
        assertThat(SuggestionValidation.MAX_TEXT_LENGTH).isBetween(100, 10000);
        assertThat(SuggestionValidation.MAX_REASON_LENGTH).isBetween(100, 5000);
        assertThat(SuggestionValidation.VALID_CONFIDENCE_VALUES)
                .containsExactlyInAnyOrder("high", "medium", "low");
    }
}