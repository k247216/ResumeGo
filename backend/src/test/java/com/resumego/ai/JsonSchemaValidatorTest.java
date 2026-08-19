package com.resumego.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JsonSchemaValidator 单元测试。
 */
class JsonSchemaValidatorTest {

    private JsonSchemaValidator validator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        validator = new JsonSchemaValidator(objectMapper);
    }

    @Test
    @DisplayName("合法 JSON 且字段齐全 → 校验通过")
    void shouldPassValidJsonWithAllRequiredFields() {
        String content = """
                {
                    "requiredSkills": ["Java"],
                    "preferredSkills": ["Docker"],
                    "responsibilities": ["后端开发"]
                }""";

        List<JsonSchemaValidator.SchemaField> schema = List.of(
                JsonSchemaValidator.SchemaField.required("requiredSkills", JsonSchemaValidator.SchemaFieldType.ARRAY),
                JsonSchemaValidator.SchemaField.optional("preferredSkills", JsonSchemaValidator.SchemaFieldType.ARRAY),
                JsonSchemaValidator.SchemaField.required("responsibilities", JsonSchemaValidator.SchemaFieldType.ARRAY)
        );

        JsonSchemaValidator.SchemaValidationResult result = validator.validate(content, schema);

        assertThat(result.isValid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    @DisplayName("缺少必填字段 → 校验失败")
    void shouldFailWhenRequiredFieldMissing() {
        String content = """
                {
                    "preferredSkills": ["Docker"]
                }""";

        List<JsonSchemaValidator.SchemaField> schema = List.of(
                JsonSchemaValidator.SchemaField.required("requiredSkills", JsonSchemaValidator.SchemaFieldType.ARRAY),
                JsonSchemaValidator.SchemaField.optional("preferredSkills", JsonSchemaValidator.SchemaFieldType.ARRAY)
        );

        JsonSchemaValidator.SchemaValidationResult result = validator.validate(content, schema);

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("requiredSkills"));
    }

    @Test
    @DisplayName("字段类型不匹配 → 校验失败")
    void shouldFailWhenFieldTypeMismatch() {
        String content = """
                {
                    "requiredSkills": "not_an_array"
                }""";

        List<JsonSchemaValidator.SchemaField> schema = List.of(
                JsonSchemaValidator.SchemaField.required("requiredSkills", JsonSchemaValidator.SchemaFieldType.ARRAY)
        );

        JsonSchemaValidator.SchemaValidationResult result = validator.validate(content, schema);

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("类型不匹配"));
    }

    @Test
    @DisplayName("非法 JSON → 校验失败")
    void shouldFailForInvalidJson() {
        String content = "not valid json at all";

        List<JsonSchemaValidator.SchemaField> schema = List.of(
                JsonSchemaValidator.SchemaField.required("field", JsonSchemaValidator.SchemaFieldType.STRING)
        );

        JsonSchemaValidator.SchemaValidationResult result = validator.validate(content, schema);

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("不是合法 JSON"));
    }

    @Test
    @DisplayName("空内容 → 校验失败")
    void shouldFailForEmptyContent() {
        List<JsonSchemaValidator.SchemaField> schema = List.of(
                JsonSchemaValidator.SchemaField.required("field", JsonSchemaValidator.SchemaFieldType.STRING)
        );

        JsonSchemaValidator.SchemaValidationResult result = validator.validate("", schema);

        assertThat(result.isValid()).isFalse();
    }

    @Test
    @DisplayName("null 内容 → 校验失败")
    void shouldFailForNullContent() {
        List<JsonSchemaValidator.SchemaField> schema = List.of(
                JsonSchemaValidator.SchemaField.required("field", JsonSchemaValidator.SchemaFieldType.STRING)
        );

        JsonSchemaValidator.SchemaValidationResult result = validator.validate(null, schema);

        assertThat(result.isValid()).isFalse();
    }

    @Test
    @DisplayName("缺少可选字段 → 校验通过")
    void shouldPassWhenOptionalFieldMissing() {
        String content = """
                {
                    "requiredSkills": ["Java"]
                }""";

        List<JsonSchemaValidator.SchemaField> schema = List.of(
                JsonSchemaValidator.SchemaField.required("requiredSkills", JsonSchemaValidator.SchemaFieldType.ARRAY),
                JsonSchemaValidator.SchemaField.optional("preferredSkills", JsonSchemaValidator.SchemaFieldType.ARRAY)
        );

        JsonSchemaValidator.SchemaValidationResult result = validator.validate(content, schema);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("数字类型校验正确")
    void shouldValidateNumberType() {
        String content = "{\"score\": 85}";

        List<JsonSchemaValidator.SchemaField> schema = List.of(
                JsonSchemaValidator.SchemaField.required("score", JsonSchemaValidator.SchemaFieldType.NUMBER)
        );

        JsonSchemaValidator.SchemaValidationResult result = validator.validate(content, schema);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("布尔类型校验正确")
    void shouldValidateBooleanType() {
        String content = "{\"valid\": true}";

        List<JsonSchemaValidator.SchemaField> schema = List.of(
                JsonSchemaValidator.SchemaField.required("valid", JsonSchemaValidator.SchemaFieldType.BOOLEAN)
        );

        JsonSchemaValidator.SchemaValidationResult result = validator.validate(content, schema);

        assertThat(result.isValid()).isTrue();
    }
}
