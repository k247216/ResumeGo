package com.resumego.interview.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.resumego.common.CurrentUser;
import com.resumego.interview.entity.InterviewerPersona;
import com.resumego.interview.mapper.InterviewerPersonaMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 面试官人设服务。
 */
@Service
public class InterviewerPersonaService {

    private static final Logger log = LoggerFactory.getLogger(InterviewerPersonaService.class);

    /** 面试官人设字段长度限制 */
    static final int MAX_PERSONA_NAME_LENGTH = 20;
    static final int MAX_PERSONA_TITLE_LENGTH = 50;
    static final int MAX_PERSONA_STYLE_LENGTH = 200;

    private final InterviewerPersonaMapper personaMapper;

    public InterviewerPersonaService(InterviewerPersonaMapper personaMapper) {
        this.personaMapper = personaMapper;
    }

    @PostConstruct
    public void initPresetPersonas() {
        long count = personaMapper.selectCount(null);
        if (count > 0) {
            return;
        }
        log.info("初始化面试官人设预设数据...");
        List<InterviewerPersona> presets = buildPresetPersonas();
        for (InterviewerPersona persona : presets) {
            personaMapper.insert(persona);
        }
        log.info("已插入 {} 个预设面试官人设", presets.size());
    }

    public List<InterviewerPersona> listPersonas() {
        QueryWrapper<InterviewerPersona> query = new QueryWrapper<>();
        query.eq("type", "preset")
                .or()
                .eq("user_id", CurrentUser.DEMO_USER_ID);
        query.orderByAsc("sort_order");
        return personaMapper.selectList(query);
    }

    @Transactional
    public InterviewerPersona createCustomPersona(String name, String title, String style) {
        // 校验：字段长度
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("姓名不能为空");
        }
        if (name.length() > MAX_PERSONA_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "姓名过长，当前 " + name.length() + " 字符，最大允许 " + MAX_PERSONA_NAME_LENGTH + " 字符");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("职位不能为空");
        }
        if (title.length() > MAX_PERSONA_TITLE_LENGTH) {
            throw new IllegalArgumentException(
                    "职位过长，当前 " + title.length() + " 字符，最大允许 " + MAX_PERSONA_TITLE_LENGTH + " 字符");
        }
        if (style == null || style.isBlank()) {
            throw new IllegalArgumentException("风格描述不能为空");
        }
        if (style.length() > MAX_PERSONA_STYLE_LENGTH) {
            throw new IllegalArgumentException(
                    "风格描述过长，当前 " + style.length() + " 字符，最大允许 " + MAX_PERSONA_STYLE_LENGTH + " 字符");
        }

        InterviewerPersona persona = new InterviewerPersona();
        persona.setName(name);
        persona.setTitle(title);
        persona.setStyle(style);
        persona.setAvatar("custom");
        persona.setType("custom");
        persona.setUserId(CurrentUser.DEMO_USER_ID);
        persona.setSortOrder(999);
        persona.setCreatedAt(LocalDateTime.now());
        persona.setUpdatedAt(LocalDateTime.now());
        personaMapper.insert(persona);
        return persona;
    }

    @Transactional
    public void deleteCustomPersona(Long id) {
        InterviewerPersona persona = personaMapper.selectById(id);
        if (persona == null) {
            throw new IllegalArgumentException("人设不存在: " + id);
        }
        if (!"custom".equals(persona.getType())) {
            throw new IllegalArgumentException("不能删除预设人设");
        }
        Long userId = persona.getUserId();
        if (userId != null && userId.longValue() != CurrentUser.DEMO_USER_ID) {
            throw new IllegalArgumentException("不能删除其他人的人设");
        }
        personaMapper.deleteById(id);
    }

    private List<InterviewerPersona> buildPresetPersonas() {
        return List.of(
                createPersona("张老师", "通用面试官", "温和专业，善于引导候选人展现真实水平，适合各类面试场景练习", "general", 1),
                createPersona("李架构", "资深后端架构师", "严谨深入，注重系统设计能力和技术深度，擅长追问技术细节", "architect", 2),
                createPersona("王经理", "HR 总监", "关注综合素质、沟通能力和文化匹配度，善于挖掘软技能", "hr", 3),
                createPersona("陈博士", "算法专家", "逻辑严密，注重问题解决思路和算法优化能力，喜欢出变形题", "algorithm", 4),
                createPersona("刘总监", "产品总监", "关注用户思维、商业敏感度和需求分析能力，常问产品设计类问题", "product", 5),
                createPersona("赵工", "前端技术专家", "注重视觉交互、用户体验和工程化实践，关注前端性能优化", "frontend", 6),
                createPersona("孙分析师", "数据科学家", "关注数据分析思维、统计方法和业务洞察力，喜欢给实际场景题", "data", 7),
                createPersona("周CEO", "创业公司 CEO", "关注主动性、学习能力和抗压能力，看重候选人的成长潜力", "startup", 8),
                createPersona("Alex Chen", "外企技术经理", "双语面试风格，注重国际化视野和跨文化沟通能力，英文提问占比高", "foreign", 9),
                createPersona("林学姐", "校招面试官", "风格轻松友好，关注潜力和成长空间，用同龄人视角进行交流", "campus", 10),
                createPersona("严总", "高压面试官", "严格犀利，高压提问，考察抗压能力和临场反应，适合进阶训练", "pressure", 11),
                createPersona("温姐", "友好面试官", "亲和力强，鼓励式提问，给出积极反馈，适合新手建立信心", "friendly", 12)
        );
    }

    private InterviewerPersona createPersona(String name, String title, String style, String avatar, int sortOrder) {
        InterviewerPersona persona = new InterviewerPersona();
        persona.setName(name);
        persona.setTitle(title);
        persona.setStyle(style);
        persona.setAvatar(avatar);
        persona.setType("preset");
        persona.setUserId(null);
        persona.setSortOrder(sortOrder);
        persona.setCreatedAt(LocalDateTime.now());
        persona.setUpdatedAt(LocalDateTime.now());
        return persona;
    }
}