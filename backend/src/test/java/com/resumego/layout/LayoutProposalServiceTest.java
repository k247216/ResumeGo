package com.resumego.layout;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumego.ai.AiClient;
import com.resumego.ai.AiClientSelector;
import com.resumego.ai.AiInvocationService;
import com.resumego.ai.AiResult;
import com.resumego.ai.validate.AiOutputValidator;
import com.resumego.layout.dto.LayoutProposalRequest;
import com.resumego.layout.dto.LayoutProposalResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 排版提案服务的回归测试。
 *
 * <p>该模块只生成可审查提案，测试固定 AI 输出和本地兜底行为，
 * 不涉及评分、匹配排序或面试状态机等禁飞区。</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LayoutProposalService 单元测试")
class LayoutProposalServiceTest {

    @Mock
    private AiClientSelector aiClientSelector;
    @Mock
    private AiClient aiClient;
    @Mock
    private AiInvocationService aiInvocationService;

    private LayoutProposalService service;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper();
        when(aiClientSelector.getClient()).thenReturn(aiClient);
        service = new LayoutProposalService(
                aiClientSelector,
                aiInvocationService,
                new AiOutputValidator(objectMapper),
                objectMapper
        );
    }

    @Test
    @DisplayName("空草稿不调用 AI，返回不可优化提示")
    void shouldReturnEmptyProposalWithoutCallingAiForEmptyDraft() {
        LayoutProposalResponse response = service.generateProposal(new LayoutProposalRequest(
                null, Map.of(), null, Map.of(), "classic", "compress_to_one_page"
        ));

        assertThat(response.model()).isEqualTo("none");
        assertThat(response.changes()).isEmpty();
        assertThat(response.warnings()).contains("当前草稿为空，无法生成排版提案");
        verify(aiClient, never()).invoke(any());
        verify(aiInvocationService, never()).logInvocationWithSchema(any(), any(), anyBoolean());
    }

    @Test
    @DisplayName("AI 返回非法 JSON 时使用本地安全兜底并记录未通过 Schema")
    void shouldFallbackWhenAiReturnsInvalidJson() {
        when(aiClient.invoke(any())).thenReturn(AiResult.success(
                "request-1", "这不是合法 JSON", 1, 1, 10
        ));
        String summary = "负责 Java 服务端模块开发，完成接口设计、异常处理和测试回归，保障功能稳定交付。";
        LayoutProposalResponse response = service.generateProposal(new LayoutProposalRequest(
                1L,
                Map.of("summary", summary),
                null,
                Map.of(),
                "classic",
                "compress_to_one_page"
        ));

        assertThat(response.model()).isEqualTo("local-fallback");
        assertThat(response.warnings()).contains("AI 输出未通过结构化校验，已使用本地安全提案兜底");
        assertThat(response.changes())
                .allSatisfy(change -> assertThat(change.riskLevel()).isEqualTo("low"));
        verify(aiInvocationService).logInvocationWithSchema(any(), any(), org.mockito.ArgumentMatchers.eq(false));
    }

    @Test
    @DisplayName("AI 调用异常时使用本地安全兜底且不丢失草稿")
    void shouldFallbackWhenAiInvocationThrows() {
        when(aiClient.invoke(any())).thenThrow(new RuntimeException("provider unavailable"));
        String summary = "负责 Java 服务端模块开发，完成接口设计、异常处理和测试回归，保障功能稳定交付并持续优化服务质量。";

        LayoutProposalResponse response = service.generateProposal(new LayoutProposalRequest(
                1L,
                Map.of("summary", summary),
                null,
                Map.of(),
                "classic",
                "compress_to_one_page"
        ));

        assertThat(response.model()).isEqualTo("local-fallback");
        assertThat(response.changes()).allSatisfy(change -> {
            assertThat(change.before()).isNotBlank();
            assertThat(change.riskLevel()).isEqualTo("low");
        });
        verify(aiInvocationService).logInvocationWithSchema(any(), any(), org.mockito.ArgumentMatchers.eq(false));
    }

    @Test
    @DisplayName("结构化输出只接受白名单模板和可验证的原文改写")
    void shouldFilterUnsafeStructuredOutput() {
        String summary = "负责 Java 服务端模块开发，并且完成接口设计、异常处理和测试回归，保障功能稳定交付并持续优化服务质量。";
        String json = """
                {
                  "changes": [{
                    "fieldKey": "summary",
                    "before": "%s",
                    "after": "负责 Java 服务端模块开发，完成接口设计、异常处理和测试回归，保障功能稳定交付并持续优化服务质量。",
                    "reason": "删除重复连接词以压缩篇幅",
                    "riskLevel": "low"
                  }, {
                    "fieldKey": "unknown.field",
                    "before": "不存在",
                    "after": "不存在",
                    "reason": "不应采纳",
                    "riskLevel": "low"
                  }],
                  "templateKey": "untrusted-template",
                  "hiddenSectionIds": ["skills", "unknown"],
                  "warnings": ["仅验证现有事实"]
                }
                """.formatted(summary);
        when(aiClient.invoke(any())).thenReturn(AiResult.success("request-2", json, 1, 1, 10));

        LayoutProposalResponse response = service.generateProposal(new LayoutProposalRequest(
                1L,
                Map.of("summary", summary),
                null,
                Map.of(),
                "classic",
                "compress_to_one_page"
        ));

        assertThat(response.model()).isEqualTo("qwen-max-or-mock");
        assertThat(response.changes()).hasSize(1);
        assertThat(response.changes().get(0).fieldKey()).isEqualTo("summary");
        assertThat(response.templateKey()).isNull();
        assertThat(response.hiddenSectionIds()).containsExactly("skills");
        verify(aiInvocationService).logInvocationWithSchema(any(), any(), org.mockito.ArgumentMatchers.eq(true));
    }

    @Test
    @DisplayName("完整草稿应提取所有可编辑模块，并过滤目标岗位中的无关字段")
    void shouldExtractAllEditableSectionsAndSanitizeTargetJob() {
        when(aiClient.invoke(any())).thenReturn(AiResult.success("request-rich", """
                {"changes": [], "templateKey": "minimal", "hiddenSectionIds": [], "warnings": ["仅做排版建议"]}
                """, 1, 1, 10));
        Map<String, Object> targetJob = new LinkedHashMap<>();
        targetJob.put("jobTitle", "后端开发工程师");
        targetJob.put("requiredSkills", List.of("Java", "Spring Boot"));
        targetJob.put("internalSecret", "不得进入 Prompt");

        LayoutProposalResponse response = service.generateProposal(new LayoutProposalRequest(
                1L, richDraft(), null, targetJob, "classic", "improve_readability"
        ));

        assertThat(response.model()).isEqualTo("qwen-max-or-mock");
        assertThat(response.templateKey()).isEqualTo("minimal");
        assertThat(response.warnings()).containsExactly("仅做排版建议");
        org.mockito.ArgumentCaptor<com.resumego.ai.AiRequest> captor =
                org.mockito.ArgumentCaptor.forClass(com.resumego.ai.AiRequest.class);
        verify(aiClient).invoke(captor.capture());
        assertThat(captor.getValue().userMessage())
                .contains("workExperience", "certifications", "githubProjects", "customSections")
                .doesNotContain("internalSecret");
    }

    @Test
    @DisplayName("本地兜底应覆盖各模块且仅隐藏未填写的白名单模块")
    void shouldBuildFallbackForRichDraftAndEmptySections() {
        when(aiClient.invoke(any())).thenReturn(AiResult.failure(
                "request-fallback", com.resumego.ai.AiErrorCategory.PROVIDER_ERROR, "down", 10));
        Map<String, Object> content = richDraft();
        content.put("activeSections", List.of("summary", "projects", "work-experience", "certifications", "languages", "github", "custom"));
        content.put("certifications", List.of());
        content.put("languages", List.of());

        LayoutProposalResponse response = service.generateProposal(new LayoutProposalRequest(
                1L, content, null, Map.of(), "classic", "compress_to_one_page"
        ));

        assertThat(response.model()).isEqualTo("local-fallback");
        assertThat(response.changes()).extracting(change -> change.sectionId())
                .contains("summary", "projects", "work-experience", "github", "custom");
        assertThat(response.hiddenSectionIds()).contains("certifications", "languages");
    }

    private Map<String, Object> richDraft() {
        String text = "负责 Java 服务端模块开发，完成接口设计、异常处理、测试回归和部署联调，保障核心功能稳定交付并持续优化用户体验。".repeat(2);
        Map<String, Object> content = new LinkedHashMap<>();
        content.put("summary", text);
        content.put("projects", List.of(Map.of("title", "职达", "description", text,
                "highlights", List.of("完成需求拆分与接口联调", "建立回归测试流程"),
                "technologies", List.of("Java", "Spring Boot", "MySQL"))));
        content.put("workExperience", List.of(Map.of("company", "校园团队", "description", text,
                "highlights", List.of("负责服务端开发与代码评审", "推动测试覆盖"),
                "technologies", List.of("Java", "Redis", "Docker"))));
        content.put("education", List.of(Map.of("school", "武汉大学",
                "highlights", List.of("完成软件工程核心课程设计", "参与团队项目协作"))));
        content.put("skillCategories", List.of(Map.of("name", "后端技术",
                "skills", List.of("Java", "Spring Boot", "MySQL", "Redis"))));
        content.put("skills", List.of("Java", "Spring Boot", "MySQL", "Redis"));
        content.put("certifications", List.of(Map.of("name", "软件设计师", "description", text)));
        content.put("languages", List.of(Map.of("name", "英语", "description", text)));
        content.put("githubProjects", List.of(Map.of("name", "ResumeGo", "description", text,
                "technologies", List.of("Vue", "Java", "MySQL"))));
        content.put("qrCodes", List.of(Map.of("name", "作品集")));
        content.put("customSections", List.of(Map.of("title", "志愿服务", "description", text)));
        return content;
    }
}
