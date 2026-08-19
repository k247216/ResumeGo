package com.resumego.layout;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumego.ai.*;
import com.resumego.ai.validate.AiOutputValidator;
import com.resumego.common.CurrentUser;
import com.resumego.layout.dto.LayoutProposalChangeDTO;
import com.resumego.layout.dto.LayoutProposalRequest;
import com.resumego.layout.dto.LayoutProposalResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI 排版助手服务。
 * <p>
 * 生成结构化提案，不直接写入简历版本或修改数据库。
 */
@Service
public class LayoutProposalService {

    private static final Logger log = LoggerFactory.getLogger(LayoutProposalService.class);

    private static final String FEATURE_TYPE = "resume_layout";
    private static final int MAX_CHANGES = 12;
    private static final Set<String> ALLOWED_TEMPLATE_KEYS = Set.of(
            "classic", "blue", "minimal", "emerald", "graphite",
            "sidebar", "compact", "elegant", "warm", "terminal"
    );
    private static final Set<String> ALLOWED_HIDDEN_SECTION_IDS = Set.of(
            "summary", "work-experience", "education", "skills", "projects",
            "certifications", "languages", "github", "qr-codes", "custom"
    );
    private static final List<String> DEFAULT_ACTIVE_SECTION_IDS = List.of(
            "personal-info", "summary", "education", "skills", "projects"
    );

    private final AiClient aiClient;
    private final AiInvocationService aiInvocationService;
    private final AiOutputValidator outputValidator;
    private final ObjectMapper objectMapper;
    private final LayoutProposalPromptBuilder promptBuilder;

    public LayoutProposalService(
            AiClientSelector aiClientSelector,
            AiInvocationService aiInvocationService,
            AiOutputValidator outputValidator,
            ObjectMapper objectMapper
    ) {
        this.aiClient = aiClientSelector.getClient();
        this.aiInvocationService = aiInvocationService;
        this.outputValidator = outputValidator;
        this.objectMapper = objectMapper;
        this.promptBuilder = new LayoutProposalPromptBuilder(objectMapper);
    }

    public LayoutProposalResponse generateProposal(LayoutProposalRequest request) {
        Map<String, Object> draftContent = request.draftContent();
        if (draftContent == null || draftContent.isEmpty()) {
            return emptyProposal(List.of("当前草稿为空，无法生成排版提案"));
        }

        List<String> activeSectionIds = resolveActiveSectionIds(draftContent);
        List<Map<String, Object>> editableFields = extractEditableFields(draftContent, activeSectionIds);
        List<String> emptySectionIds = extractEmptySectionIds(draftContent, activeSectionIds);

        if (editableFields.isEmpty() && emptySectionIds.isEmpty()) {
            return emptyProposal(List.of("未找到可优化的排版文本"));
        }

        String systemPrompt = promptBuilder.buildSystemPrompt();
        String userMessage = promptBuilder.buildUserMessage(promptBuilder.buildPayload(
                request.templateKey(),
                request.goal(),
                sanitizeTargetJob(request.targetJob()),
                activeSectionIds,
                editableFields,
                emptySectionIds
        ));

        String requestId = UUID.randomUUID().toString();
        AiRequest aiRequest = AiRequest.builder()
                .requestId(requestId)
                .featureType(FEATURE_TYPE)
                .userId(CurrentUser.DEMO_USER_ID)
                .promptVersion(LayoutProposalPromptBuilder.PROMPT_VERSION)
                .systemPrompt(systemPrompt)
                .userMessage(userMessage)
                .build();

        long start = System.currentTimeMillis();
        AiResult aiResult;
        try {
            aiResult = aiClient.invoke(aiRequest);
        } catch (Exception e) {
            log.error("AI 排版助手调用异常: requestId={}", requestId, e);
            aiResult = AiResult.failure(requestId, AiErrorCategory.PROVIDER_ERROR,
                    "AI 服务异常，请稍后重试", System.currentTimeMillis() - start);
        }

        boolean schemaValid = false;
        LayoutProposalResponse response = null;
        if (aiResult.success() && aiResult.content() != null) {
            try {
                String json = outputValidator.extractJson(aiResult.content());
                var jsonResult = outputValidator.validateJson(json);
                if (jsonResult.isValid()) {
                    schemaValid = true;
                    response = parseAndValidate(json, editableFields, emptySectionIds);
                }
            } catch (Exception e) {
                log.warn("AI 排版助手输出校验失败: requestId={}", requestId, e);
                schemaValid = false;
            }
        }

        aiInvocationService.logInvocationWithSchema(aiRequest, aiResult, schemaValid);

        if (response == null) {
            return fallbackProposal(editableFields, emptySectionIds, request.templateKey(),
                    List.of("AI 输出未通过结构化校验，已使用本地安全提案兜底"));
        }

        return response;
    }

    private LayoutProposalResponse parseAndValidate(
            String json,
            List<Map<String, Object>> editableFields,
            List<String> emptySectionIds
    ) throws Exception {
        JsonNode root = objectMapper.readTree(json);
        Map<String, String> fieldTextByKey = new HashMap<>();
        Map<String, String> labelByKey = new HashMap<>();
        Map<String, String> sectionByKey = new HashMap<>();
        for (Map<String, Object> field : editableFields) {
            String fieldKey = String.valueOf(field.get("fieldKey"));
            fieldTextByKey.put(fieldKey, String.valueOf(field.get("text")));
            labelByKey.put(fieldKey, String.valueOf(field.get("label")));
            sectionByKey.put(fieldKey, sectionIdFromFieldKey(fieldKey));
        }

        List<LayoutProposalChangeDTO> changes = new ArrayList<>();
        JsonNode changesNode = root.get("changes");
        if (changesNode != null && changesNode.isArray()) {
            int index = 0;
            for (JsonNode node : changesNode) {
                if (changes.size() >= MAX_CHANGES) break;
                String fieldKey = text(node, "fieldKey");
                String before = text(node, "before");
                String after = text(node, "after");
                String reason = text(node, "reason");
                String riskLevel = Optional.ofNullable(text(node, "riskLevel")).orElse("low");

                if (!fieldTextByKey.containsKey(fieldKey)) continue;
                String original = fieldTextByKey.get(fieldKey);
                if (before == null || after == null || reason == null) continue;
                if (!original.contains(before)) continue;
                if (!isSafeRewrite(before, after)) continue;
                if (!"low".equals(riskLevel) && !"medium".equals(riskLevel)) continue;

                changes.add(new LayoutProposalChangeDTO(
                        "layout-" + index++,
                        sectionByKey.get(fieldKey),
                        fieldKey,
                        labelByKey.getOrDefault(fieldKey, fieldKey),
                        before,
                        after,
                        reason,
                        riskLevel
                ));
            }
        }

        String templateKey = text(root, "templateKey");
        if (templateKey != null && !ALLOWED_TEMPLATE_KEYS.contains(templateKey)) {
            templateKey = null;
        }

        List<String> hiddenSectionIds = parseStringArray(root.get("hiddenSectionIds")).stream()
                .filter(emptySectionIds::contains)
                .filter(ALLOWED_HIDDEN_SECTION_IDS::contains)
                .distinct()
                .toList();

        List<String> warnings = parseStringArray(root.get("warnings")).stream()
                .limit(5)
                .toList();

        return new LayoutProposalResponse(
                UUID.randomUUID().toString(),
                "qwen-max-or-mock",
                LayoutProposalPromptBuilder.PROMPT_VERSION,
                changes,
                templateKey,
                hiddenSectionIds,
                warnings
        );
    }

    private List<Map<String, Object>> extractEditableFields(Map<String, Object> content, List<String> activeSectionIds) {
        List<Map<String, Object>> fields = new ArrayList<>();

        if (activeSectionIds.contains("summary")) {
            addTextField(fields, "summary", "个人简介", content.get("summary"));
        }

        if (activeSectionIds.contains("projects")) {
            List<Map<String, Object>> projects = asMapList(content.get("projects"));
            for (int i = 0; i < projects.size(); i++) {
                Map<String, Object> project = projects.get(i);
                String label = firstNonBlank(project.get("title"), project.get("name"), "项目经历 " + (i + 1));
                addTextField(fields, "projects." + i + ".description", label, project.get("description"));
                addListField(fields, "projects." + i + ".highlights", label + "｜项目亮点", project.get("highlights"));
                addListField(fields, "projects." + i + ".technologies", label + "｜技术栈", project.get("technologies"));
            }
        }

        if (activeSectionIds.contains("work-experience")) {
            List<Map<String, Object>> workExperiences = asMapList(content.get("workExperience"));
            for (int i = 0; i < workExperiences.size(); i++) {
                Map<String, Object> item = workExperiences.get(i);
                String label = firstNonBlank(item.get("company"), item.get("position"), "工作经历 " + (i + 1));
                addTextField(fields, "workExperience." + i + ".description", label, item.get("description"));
                addListField(fields, "workExperience." + i + ".highlights", label + "｜亮点成果", item.get("highlights"));
                addListField(fields, "workExperience." + i + ".technologies", label + "｜技术栈", item.get("technologies"));
            }
        }

        if (activeSectionIds.contains("education")) {
            List<Map<String, Object>> education = asMapList(content.get("education"));
            for (int i = 0; i < education.size(); i++) {
                Map<String, Object> item = education.get(i);
                String label = firstNonBlank(item.get("school"), item.get("institution"), "教育背景 " + (i + 1));
                addListField(fields, "education." + i + ".highlights", label + "｜在校亮点", item.get("highlights"));
            }
        }

        if (activeSectionIds.contains("skills")) {
            List<Map<String, Object>> skillCategories = asMapList(content.get("skillCategories"));
            for (int i = 0; i < skillCategories.size(); i++) {
                Map<String, Object> category = skillCategories.get(i);
                String label = firstNonBlank(category.get("name"), null, "技能类别 " + (i + 1));
                addListField(fields, "skillCategories." + i + ".skills", label + "｜技能项", category.get("skills"));
            }
            addListField(fields, "skills", "技能特长", content.get("skills"));
        }

        if (activeSectionIds.contains("certifications")) {
            List<Map<String, Object>> certifications = asMapList(content.get("certifications"));
            for (int i = 0; i < certifications.size(); i++) {
                Map<String, Object> item = certifications.get(i);
                String label = firstNonBlank(item.get("name"), item.get("issuer"), "资格证书 " + (i + 1));
                addTextField(fields, "certifications." + i + ".description", label, item.get("description"));
            }
        }

        if (activeSectionIds.contains("languages")) {
            List<Map<String, Object>> languages = asMapList(content.get("languages"));
            for (int i = 0; i < languages.size(); i++) {
                Map<String, Object> item = languages.get(i);
                String label = firstNonBlank(item.get("name"), item.get("level"), "语言能力 " + (i + 1));
                addTextField(fields, "languages." + i + ".description", label, item.get("description"));
            }
        }

        if (activeSectionIds.contains("github")) {
            List<Map<String, Object>> githubProjects = asMapList(content.get("githubProjects"));
            for (int i = 0; i < githubProjects.size(); i++) {
                Map<String, Object> item = githubProjects.get(i);
                String label = firstNonBlank(item.get("name"), item.get("url"), "GitHub 项目 " + (i + 1));
                addTextField(fields, "githubProjects." + i + ".description", label, item.get("description"));
                addListField(fields, "githubProjects." + i + ".technologies", label + "｜技术栈", item.get("technologies"));
            }
        }

        if (activeSectionIds.contains("custom")) {
            List<Map<String, Object>> customSections = asMapList(content.get("customSections"));
            for (int i = 0; i < customSections.size(); i++) {
                Map<String, Object> item = customSections.get(i);
                String label = firstNonBlank(item.get("title"), null, "自定义模块 " + (i + 1));
                addTextField(fields, "customSections." + i + ".description", label, item.get("description"));
            }
        }

        return fields;
    }

    private List<String> extractEmptySectionIds(Map<String, Object> content, List<String> activeSectionIds) {
        List<String> empty = new ArrayList<>();
        Set<String> active = new LinkedHashSet<>(activeSectionIds);
        Set<String> hidden = new LinkedHashSet<>(asStringList(content.get("hiddenSections")));

        if (active.contains("summary") && !hidden.contains("summary") && isBlank(content.get("summary"))) empty.add("summary");
        if (active.contains("work-experience") && !hidden.contains("work-experience") && asMapList(content.get("workExperience")).isEmpty()) empty.add("work-experience");
        if (active.contains("education") && !hidden.contains("education") && asMapList(content.get("education")).isEmpty()) empty.add("education");
        if (active.contains("projects") && !hidden.contains("projects") && asMapList(content.get("projects")).isEmpty()) empty.add("projects");
        if (active.contains("skills") && !hidden.contains("skills") && asStringList(content.get("skills")).isEmpty()
                && asMapList(content.get("skillCategories")).isEmpty()) empty.add("skills");
        if (active.contains("certifications") && !hidden.contains("certifications") && asMapList(content.get("certifications")).isEmpty()) empty.add("certifications");
        if (active.contains("languages") && !hidden.contains("languages") && asMapList(content.get("languages")).isEmpty()) empty.add("languages");
        if (active.contains("github") && !hidden.contains("github") && asMapList(content.get("githubProjects")).isEmpty()) empty.add("github");
        if (active.contains("qr-codes") && !hidden.contains("qr-codes") && asMapList(content.get("qrCodes")).isEmpty()) empty.add("qr-codes");
        if (active.contains("custom") && !hidden.contains("custom") && asMapList(content.get("customSections")).isEmpty()) empty.add("custom");
        return empty;
    }

    private void addTextField(List<Map<String, Object>> fields, String fieldKey, String label, Object value) {
        if (isBlank(value)) return;
        String text = String.valueOf(value).trim();
        if (text.length() < 30) return;
        fields.add(Map.of(
                "fieldKey", fieldKey,
                "sectionId", sectionIdFromFieldKey(fieldKey),
                "label", label,
                "text", text
        ));
    }

    private void addListField(List<Map<String, Object>> fields, String fieldKey, String label, Object value) {
        List<String> values = asStringList(value);
        if (values.isEmpty()) return;
        String text = String.join("、", values);
        if (text.length() < 12) return;
        fields.add(Map.of(
                "fieldKey", fieldKey,
                "sectionId", sectionIdFromFieldKey(fieldKey),
                "label", label,
                "text", text
        ));
    }

    private boolean isSafeRewrite(String before, String after) {
        if (after.isBlank()) return false;
        if (after.length() > Math.ceil(before.length() * 1.15)) return false;
        return numericTokens(after).stream().allMatch(numericTokens(before)::contains);
    }

    private Set<String> numericTokens(String text) {
        Set<String> tokens = new HashSet<>();
        Matcher matcher = Pattern.compile("\\d+(?:\\.\\d+)?%?").matcher(text == null ? "" : text);
        while (matcher.find()) {
            tokens.add(matcher.group());
        }
        return tokens;
    }

    private LayoutProposalResponse fallbackProposal(
            List<Map<String, Object>> editableFields,
            List<String> emptySectionIds,
            String currentTemplateKey,
            List<String> warnings
    ) {
        List<LayoutProposalChangeDTO> changes = new ArrayList<>();
        Set<String> usedSections = new LinkedHashSet<>();
        for (Map<String, Object> field : editableFields) {
            if (changes.size() >= MAX_CHANGES) break;
            String sectionId = String.valueOf(field.get("sectionId"));
            if (usedSections.contains(sectionId)) continue;
            String before = String.valueOf(field.get("text"));
            String after = compactText(before, Math.min(180, Math.max(80, before.length() - 20)));
            if (after.equals(before)) continue;
            changes.add(new LayoutProposalChangeDTO(
                    "fallback-" + field.get("fieldKey"),
                    sectionId,
                    String.valueOf(field.get("fieldKey")),
                    String.valueOf(field.get("label")),
                    before,
                    after,
                    "本地安全兜底：按模块压缩表达以降低排版密度",
                    "low"
            ));
            usedSections.add(sectionId);
        }

        String templateKey = null;
        List<String> hidden = emptySectionIds.stream()
                .filter(ALLOWED_HIDDEN_SECTION_IDS::contains)
                .limit(4)
                .toList();

        return new LayoutProposalResponse(
                UUID.randomUUID().toString(),
                "local-fallback",
                LayoutProposalPromptBuilder.PROMPT_VERSION,
                changes,
                templateKey,
                hidden,
                warnings
        );
    }

    private LayoutProposalResponse emptyProposal(List<String> warnings) {
        return new LayoutProposalResponse(
                UUID.randomUUID().toString(),
                "none",
                LayoutProposalPromptBuilder.PROMPT_VERSION,
                List.of(),
                null,
                List.of(),
                warnings
        );
    }

    private String compactText(String text, int maxLength) {
        String normalized = text.replaceAll("\\s+", " ")
                .replace("，并且", "，")
                .replace("进行了", "完成")
                .trim();
        if (normalized.length() <= maxLength) return normalized;
        return normalized.substring(0, maxLength).trim() + "…";
    }

    private String sectionIdFromFieldKey(String fieldKey) {
        if (fieldKey == null) return "summary";
        if (fieldKey.startsWith("projects.")) return "projects";
        if (fieldKey.startsWith("workExperience.")) return "work-experience";
        if (fieldKey.startsWith("education.")) return "education";
        if (fieldKey.startsWith("skillCategories.") || fieldKey.equals("skills")) return "skills";
        if (fieldKey.startsWith("certifications.")) return "certifications";
        if (fieldKey.startsWith("languages.")) return "languages";
        if (fieldKey.startsWith("githubProjects.")) return "github";
        if (fieldKey.startsWith("customSections.")) return "custom";
        if (fieldKey.startsWith("summary")) return "summary";
        return "summary";
    }

    private List<String> resolveActiveSectionIds(Map<String, Object> content) {
        Set<String> allowed = new LinkedHashSet<>(ALLOWED_HIDDEN_SECTION_IDS);
        allowed.add("personal-info");

        List<String> stored = asStringList(content.get("activeSections")).stream()
                .filter(allowed::contains)
                .distinct()
                .toList();
        if (!stored.isEmpty()) {
            LinkedHashSet<String> ids = new LinkedHashSet<>();
            ids.add("personal-info");
            ids.addAll(stored);
            return new ArrayList<>(ids);
        }

        LinkedHashSet<String> ids = new LinkedHashSet<>(DEFAULT_ACTIVE_SECTION_IDS);
        if (!asMapList(content.get("workExperience")).isEmpty()) ids.add("work-experience");
        if (!asMapList(content.get("certifications")).isEmpty()) ids.add("certifications");
        if (!asMapList(content.get("languages")).isEmpty()) ids.add("languages");
        if (!asMapList(content.get("githubProjects")).isEmpty()) ids.add("github");
        if (!asMapList(content.get("qrCodes")).isEmpty()) ids.add("qr-codes");
        if (!asMapList(content.get("customSections")).isEmpty()) ids.add("custom");
        return new ArrayList<>(ids);
    }

    private Map<String, Object> sanitizeTargetJob(Map<String, Object> targetJob) {
        if (targetJob == null || targetJob.isEmpty()) return Map.of();
        Map<String, Object> sanitized = new LinkedHashMap<>();
        copyIfPresent(targetJob, sanitized, "jobTitle");
        copyIfPresent(targetJob, sanitized, "companyName");
        copyIfPresent(targetJob, sanitized, "requiredSkills");
        copyIfPresent(targetJob, sanitized, "preferredSkills");
        copyIfPresent(targetJob, sanitized, "responsibilities");
        copyIfPresent(targetJob, sanitized, "experienceRequirements");
        copyIfPresent(targetJob, sanitized, "educationRequirements");
        return sanitized;
    }

    private void copyIfPresent(Map<String, Object> source, Map<String, Object> target, String key) {
        Object value = source.get(key);
        if (value != null) {
            target.put(key, value);
        }
    }

    private String text(JsonNode node, String fieldName) {
        JsonNode value = node == null ? null : node.get(fieldName);
        return value == null || value.isNull() ? null : value.asText();
    }

    private List<String> parseStringArray(JsonNode node) {
        if (node == null || !node.isArray()) return List.of();
        List<String> values = new ArrayList<>();
        node.forEach(item -> {
            if (item != null && item.isTextual() && !item.asText().isBlank()) {
                values.add(item.asText());
            }
        });
        return values;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> asMapList(Object value) {
        if (!(value instanceof List<?> list)) return List.of();
        return list.stream()
                .filter(Map.class::isInstance)
                .map(item -> objectMapper.convertValue(item, new TypeReference<Map<String, Object>>() {}))
                .toList();
    }

    private List<String> asStringList(Object value) {
        if (!(value instanceof List<?> list)) return List.of();
        return list.stream()
                .filter(Objects::nonNull)
                .map(String::valueOf)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private boolean isBlank(Object value) {
        return value == null || String.valueOf(value).trim().isBlank();
    }

    private String firstNonBlank(Object first, Object second, String fallback) {
        if (!isBlank(first)) return String.valueOf(first);
        if (!isBlank(second)) return String.valueOf(second);
        return fallback;
    }
}
