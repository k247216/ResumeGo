# 职达 Career OS 产品模型冻结 v1.1

Version: V2.0
Status: Frozen active direction

## 核心定位

职达不是一个 AI 求职工具，而是一个围绕个人职业资产，通过目标管理、知识沉淀、面试训练和反馈成长，帮助用户持续提升职业竞争力的 Career OS。

核心价值顺序：

```text
Knowledge Base
      ↓
Career Pipeline
      ↓
Resume Version
      ↓
Interview Engine
      ↓
Growth Feedback
      ↓
Workspace Action
```

Knowledge Base 是长期资产层。其他模块引用它并产生价值，但不能改变其事实边界。

## Workspace

Workspace 是用户每天打开软件时的行动入口，只回答：

> 我现在最应该做什么？

信息权重：

- 今日行动 70%；
- 最近活动 20%；
- 成长反馈 10%。

今日行动可以来自临近面试、明确待办、知识训练、面试反馈或简历改进。首页不承担完整编辑、完整模拟面试、趋势分析、日历管理和 Pipeline 详情。

成长反馈只用于温和鼓励，不使用排行榜、游戏化分数或惩罚性连续天数。

## Career Pipeline

Career Pipeline 是一个真实岗位机会的求职管线，不是知识容器。

每条 Pipeline 独立拥有：

- 公司、岗位和 JD；
- 生命周期状态；
- 用户可配置的阶段及阶段历史；
- 关联简历版本；
- 面试记录与日程；
- 反馈引用；
- 下一步行动。

Java、Redis、JVM 等通用知识属于 Knowledge Base。Pipeline 只能引用这些资产，不能复制并据为己有。

## Knowledge Base

Knowledge Base 是 Career OS 的核心职业资产层。

内容类型包括：

- 技术知识：Java、Redis、JVM 等；
- 项目知识：项目材料、设计与复盘；
- 面试资产：真实面经、用户题库、历史问题；
- 个人总结：学习笔记、错题和反思；
- 用户确认的能力事实。

它服务学习、面试和岗位准备。资料中提及某项技能，不代表用户已经掌握；AI 推断必须保持为建议，只有用户确认后才能成为能力事实。

第一版完成文件导入、分类、搜索、原文定位和带引用的基础 RAG，不追求复杂 Agent 和知识图谱。

## Resume System

Resume System 是职业表达层：

```text
Knowledge Base
      ↓
用户确认的能力事实
      ↓
基础简历资产
      ↓
岗位表达版本
```

基础职业资产可以复用，岗位版本独立演进。修改腾讯岗位版本不得污染字节岗位版本。AI 建议保留原文、依据、建议稿、差异和用户决定，不得静默覆盖。

## Interview Engine

面试引擎固定为三种模式：

### Role Based Interview

来源为 Pipeline、Resume Version 和面试官角色，用于岗位适配型开放训练。

### Knowledge Training Interview

来源为用户主动选择的 Knowledge Base 内容，用于学习、复习和查漏补缺。

### Experience Simulation Interview

来源为真实面经、用户题库或历史问题。AI 扮演主持人，负责提问、追问和评价，不能将生成问题伪装为真实面经。

三种模式共享会话和反馈基础设施，但保留独立来源契约与评价规则。

## Growth System

Growth System 是横向反馈层，不是独立核心工作区。

它从 Interview Feedback、Learning Record 和 Resume Improvement 中产生：

- 能力趋势；
- 薄弱点；
- 下一步建议。

Growth 不拥有原始事实，不决定最终分数，不做排行榜和游戏化管理。

## External Action Layer

投递能力作为 Pipeline 外围能力保留，可以记录投递时间、状态和平台来源，未来可以辅助填写。它不成为核心竞争力，不自动提交申请，也不扩展为招聘平台或岗位爬虫。

## 最终闭环

```text
Knowledge Base
      ↓
用户职业资产
      ↓
Career Pipeline
      ↓
Resume Version
      ↓
Interview Engine
      ↓
Feedback
      ↓
Growth System
      ↓
Workspace Next Action
      ↓
下一轮提升
```

## 当前优先级

### P0

1. Career Pipeline 完整化；
2. Knowledge Base 第一版；
3. Interview Engine 基于 Pipeline、Resume 和 Knowledge 重构。

### P1

- Resume Version 完善；
- Growth Feedback；
- Calendar Sync。

### P2

- Skill / MCP；
- Agent；
- 投递辅助。

## 非目标

- 公共岗位市场和岗位爬虫；
- 自动投递平台；
- AI 编造用户经历或能力；
- 没有来源的职业事实；
- 排行榜和游戏化能力评分；
- 在核心领域未稳定前进行全面 UI 重做；
- 以云端账户或同步作为本地使用前提。

## 当前版本策略

V1 是独立维护的已发布产品。V2 是当前新功能开发线，早期与 V1 并行安装，使用独立应用标识和数据目录。V2 通过显式迁移导入 V1 数据副本，不原地修改 V1 工作区。
