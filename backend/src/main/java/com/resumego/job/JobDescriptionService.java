package com.resumego.job;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resumego.ai.JdParseService;
import com.resumego.common.CurrentUser;
import com.resumego.job.dto.CreateJobDescriptionRequest;
import com.resumego.job.dto.JobDescriptionDTO;
import com.resumego.job.dto.ParsedJobDescriptionDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 本地单用户版本固定使用 userId=1 作为数据归属。
 * 若未来引入多用户能力，再从认证上下文获取当前用户 ID。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JobDescriptionService {

    /** JD 正文最大长度（字符数），防止超长文本导致 AI Token 超额 */
    private static final int MAX_JD_RAW_TEXT_LENGTH = 50_000;

    private final JobDescriptionMapper jobDescriptionMapper;
    private final JdParseService jdParseService;
    private final ObjectMapper objectMapper;

    @Transactional
    public JobDescriptionDTO create(CreateJobDescriptionRequest req) {
        // 校验：空内容或纯空白内容
        String rawText = req.getRawText();
        if (rawText == null || rawText.isBlank()) {
            throw new IllegalArgumentException("JD 正文不能为空");
        }

        // 校验：内容长度超限
        if (rawText.length() > MAX_JD_RAW_TEXT_LENGTH) {
            throw new IllegalArgumentException(
                    "JD 正文过长，当前 " + rawText.length() + " 字符，最大允许 " + MAX_JD_RAW_TEXT_LENGTH + " 字符");
        }

        JobDescription entity = new JobDescription();
        entity.setUserId(CurrentUser.DEMO_USER_ID);
        entity.setJobTitle(req.getJobTitle());
        entity.setCompanyName(req.getCompanyName());
        entity.setRawText(rawText);
        entity.setParseStatus("pending");
        entity.setSourceMetaJson(req.getSourceMetaJson());
        entity.setJobType(req.getJobType());

        jobDescriptionMapper.insert(entity);
        log.info("JD created: id={}, title={}, userId={}",
                entity.getId(), entity.getJobTitle(), CurrentUser.DEMO_USER_ID);

        return toDTO(entity);
    }

    @Transactional
    public JobDescriptionDTO parse(Long jobDescriptionId) {
        JobDescription entity = jobDescriptionMapper.selectById(jobDescriptionId);
        if (entity == null) {
            return null;
        }

        String rawText = entity.getRawText();
        if (rawText == null || rawText.isBlank()) {
            log.warn("JD parse skipped: rawText is empty, id={}", jobDescriptionId);
            entity.setParseStatus("failed");
            jobDescriptionMapper.updateById(entity);
            return toDTO(entity);
        }

        try {
            ParsedJobDescriptionDTO parsed = jdParseService.parse(rawText);

            // 直接从 sourceMeta.tags 提取技能标签
            List<String> tagsFromMeta = extractTagsFromSourceMeta(entity.getSourceMetaJson());
            if (!tagsFromMeta.isEmpty()) {
                parsed.setRequiredSkills(tagsFromMeta);
            }

            String parsedJson = objectMapper.writeValueAsString(parsed);

            entity.setParsedJson(parsedJson);
            entity.setParseStatus("succeeded");
            entity.setPromptVersion("stub-v1");

            jobDescriptionMapper.updateById(entity);
            log.info("JD parsed: id={}, skills={}",
                    jobDescriptionId,
                    parsed.getRequiredSkills() != null ? parsed.getRequiredSkills().size() : 0);

        } catch (JsonProcessingException e) {
            log.error("JD parse: JSON 序列化失败, id={}", jobDescriptionId, e);
            markFailed(entity);
        } catch (Exception e) {
            log.error("JD parse: 解析失败, id={}", jobDescriptionId, e);
            markFailed(entity);
        }

        return toDTO(entity);
    }

    private void markFailed(JobDescription entity) {
        entity.setParseStatus("failed");
        jobDescriptionMapper.updateById(entity);
    }

    public JobDescriptionDTO findById(Long id) {
        JobDescription entity = jobDescriptionMapper.selectById(id);
        return entity != null ? toDTO(entity) : null;
    }

    /**
     * 查询当前本地用户的所有 JD，按创建时间倒序。
     */
    public List<JobDescriptionDTO> findAllByUser() {
        QueryWrapper<JobDescription> query = new QueryWrapper<>();
        query.eq("user_id", CurrentUser.DEMO_USER_ID)
             .orderByDesc("created_at");
        return jobDescriptionMapper.selectList(query).stream()
                .map(this::toDTO)
                .toList();
    }

    /**
     * 将 Entity 转为 DTO。
     */
    private JobDescriptionDTO toDTO(JobDescription entity) {
        JobDescriptionDTO dto = new JobDescriptionDTO();
        dto.setId(entity.getId());
        dto.setJobTitle(entity.getJobTitle());
        dto.setCompanyName(entity.getCompanyName());
        dto.setRawText(entity.getRawText());
        dto.setParsed(deserializeJson(entity.getParsedJson(), ParsedJobDescriptionDTO.class));
        dto.setParseStatus(entity.getParseStatus());
        dto.setPromptVersion(entity.getPromptVersion());
        dto.setJobType(entity.getJobType());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        dto.setSourceMeta(deserializeJson(entity.getSourceMetaJson(), Object.class));
        return dto;
    }

    /**
     * 重新解析当前用户的所有 JD。
     */
    @Transactional
    public int reparseAll() {
        List<JobDescription> all = jobDescriptionMapper.selectList(
                new QueryWrapper<JobDescription>()
                        .eq("user_id", CurrentUser.DEMO_USER_ID)
        );

        int count = 0;
        for (JobDescription entity : all) {
            String rawText = entity.getRawText();
            if (rawText == null || rawText.isBlank()) {
                continue;
            }
            try {
                ParsedJobDescriptionDTO parsed = jdParseService.parse(rawText);
                List<String> tagsFromMeta = extractTagsFromSourceMeta(entity.getSourceMetaJson());
                if (!tagsFromMeta.isEmpty()) {
                    parsed.setRequiredSkills(tagsFromMeta);
                }
                entity.setParsedJson(objectMapper.writeValueAsString(parsed));
                entity.setParseStatus("succeeded");
                entity.setPromptVersion("stub-v2");
                jobDescriptionMapper.updateById(entity);
                count++;
            } catch (Exception e) {
                log.error("JD reparse failed: id={}", entity.getId(), e);
                entity.setParseStatus("failed");
                jobDescriptionMapper.updateById(entity);
            }
        }
        log.info("JD batch reparse done: {}/{}", count, all.size());
        return count;
    }

    @SuppressWarnings("unchecked")
    private List<String> extractTagsFromSourceMeta(String sourceMetaJson) {
        if (sourceMetaJson == null || sourceMetaJson.isBlank()) {
            return List.of();
        }
        try {
            java.util.Map<String, Object> meta = objectMapper.readValue(sourceMetaJson, java.util.Map.class);
            Object tags = meta.get("tags");
            if (tags instanceof java.util.List) {
                return ((java.util.List<?>) tags).stream()
                        .filter(t -> t instanceof String)
                        .map(t -> (String) t)
                        .toList();
            }
        } catch (JsonProcessingException e) {
            log.warn("Failed to extract tags from sourceMetaJson", e);
        }
        return List.of();
    }

    @Transactional
    public int batchUpdateJobType(String jobType) {
        List<JobDescription> all = jobDescriptionMapper.selectList(
                new QueryWrapper<JobDescription>()
                        .eq("user_id", CurrentUser.DEMO_USER_ID)
        );
        for (JobDescription entity : all) {
            entity.setJobType(jobType);
            jobDescriptionMapper.updateById(entity);
        }
        log.info("Batch update jobType to '{}': {} records", jobType, all.size());
        return all.size();
    }

    @Transactional
    public boolean delete(Long id) {
        JobDescription entity = jobDescriptionMapper.selectById(id);
        if (entity == null) {
            return false;
        }
        jobDescriptionMapper.deleteById(id);
        log.info("JD deleted: id={}", id);
        return true;
    }

    private <T> T deserializeJson(String json, Class<T> clazz) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            log.warn("JSON 反序列化失败: {}", clazz.getSimpleName(), e);
            return null;
        }
    }
}
