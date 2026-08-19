package com.resumego.ai.validate;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * AiOutputValidator 单元测试。
 */
class AiOutputValidatorTest {

    private AiOutputValidator validator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        validator = new AiOutputValidator(objectMapper);
    }

    @Test
    @DisplayName("validateFieldValue: 值在允许范围内 → 通过")
    void shouldPassWhenValueInAllowedSet() {
        String json = "{\"type\": \"technical\"}";
        var result = validator.validateFieldValue(json, "type",
                Set.of("behavioral", "technical", "situational"));
        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("validateFieldValue: 值不在允许范围内 → 失败")
    void shouldFailWhenValueNotInAllowedSet() {
        String json = "{\"type\": \"unknown\"}";
        var result = validator.validateFieldValue(json, "type",
                Set.of("behavioral", "technical", "situational"));
        assertThat(result.isValid()).isFalse();
    }

    @Test
    @DisplayName("validateFieldValue: 字段不存在 → 失败")
    void shouldFailWhenFieldMissing() {
        String json = "{\"other\": \"value\"}";
        var result = validator.validateFieldValue(json, "type",
                Set.of("behavioral"));
        assertThat(result.isValid()).isFalse();
    }

    @Test
    @DisplayName("validateFieldValue: 字段非字符串 → 失败")
    void shouldFailWhenFieldNotString() {
        String json = "{\"type\": 123}";
        var result = validator.validateFieldValue(json, "type",
                Set.of("behavioral"));
        assertThat(result.isValid()).isFalse();
    }
}