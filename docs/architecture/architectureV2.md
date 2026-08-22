
# 职达 Career OS 技术架构说明

Version: V2.0

Status: Approved architecture direction (V2, current development line)

## Current implementation boundary

The first V2 backend boundary is `/api/v2/pipelines`, implemented by the isolated `com.resumego.pipeline` module. It uses additive H2 V4 and MySQL V24 migrations for `career_pipelines`, `pipeline_stages` and append-only `pipeline_stage_transitions`.

The legacy `/api/v1/projects` API and `job_projects` table remain unchanged and serve only V1 maintenance and future explicit migration. Current V2 API support covers creation, reads, initial custom stages, post-creation stage add/rename/reorder, deterministic stage transitions, archive and restore. Stage reordering is an all-stage atomic operation so persisted positions remain unique. Schedule/interview links, V1 import and the Pipeline UI remain later F1 slices.


# 1. Architecture Overview


## 1.1 产品技术定位


职达 Career OS 采用：

Local-first Desktop Application Architecture


核心原则：

- 用户数据默认保存在本地
- AI 能力作为增强服务
- 核心业务逻辑不依赖模型
- 模型输出必须经过业务校验
- 用户始终拥有数据控制权


---

# 2. Overall Architecture


┌───────────────────────────┐
│        Electron Shell      │
│                            │
│ Window / IPC / Storage     │
│ File / Update / Security   │
└─────────────┬─────────────┘
              │
              │ IPC + Local HTTP
              │
┌─────────────▼─────────────┐
│       Vue Renderer         │
│                            │
│ Workspace                  │
│ Pipeline                   │
│ Resume                     │
│ Interview                  │
│ Calendar                   │
└─────────────┬─────────────┘
              │
              │
┌─────────────▼─────────────┐
│     Spring Boot Local      │
│     Modular Monolith       │
│                            │
│ Career                     │
│ Resume                     │
│ Knowledge                  │
│ Interview                  │
│ Calendar                   │
│ Growth                     │
│ AI                         │
└─────────────┬─────────────┘
              │
              │
┌─────────────▼─────────────┐
│ Local Data Layer           │
│                            │
│ H2 Database                │
│ File Storage               │
│ Vector Storage             │
└───────────────────────────┘
External:
LLM API
Embedding API
Speech API
Calendar Provider


---

# 3. Runtime Architecture


## 3.1 Electron Main Process


职责：

- 创建应用窗口
- 管理生命周期
- 启动 Spring Boot 子进程
- 管理本地数据目录
- 文件导入导出
- 安全存储 API Key
- 应用更新


不负责：

- 简历分析
- 面试逻辑
- AI Prompt
- 数据业务规则


---

## 3.2 Vue Renderer


职责：

负责用户交互和视觉展示。


核心页面：

Workspace
Career Pipeline
Resume Library
Interview Center
Calendar
Settings


特点：

- 不直接访问文件系统
- 不直接调用 Node API
- 通过 HTTP / IPC 请求能力


---

## 3.3 Spring Boot Local Service


作为本地业务核心。


负责：

- 领域逻辑
- 数据校验
- 状态管理
- AI调用编排
- 数据持久化


运行方式：

Electron 启动时自动启动。


监听：

localhost


使用：

随机端口

Session Token


---

# 4. Domain Architecture


系统采用模块化单体。


模块之间：

通过 Service Interface 通信。


禁止：

跨模块直接操作数据库。


---

# 5. Core Business Modules


## 5.1 Career Module


职责：

管理用户求职机会。


核心对象：

CareerPipeline
JobTarget
PipelineStage


负责：

- 公司
- 岗位
- JD
- 当前阶段
- 岗位状态


示例：

Tencent Backend
Prepare
↓
Online Test
↓
Technical Interview
↓
HR
↓
Offer


---

# 5.2 Resume Module


职责：

管理职业表达资产。


核心对象：

Resume
ResumeVersion
Evidence
ProjectExperience


数据关系：


Base Resume
 |
 |
 Resume Version
Tencent Backend V3
Byte Backend V2


负责：

- 导入
- 编辑
- 版本控制
- 差异比较
- 导出 PDF


---

# 5.3 Knowledge Module


新增核心模块。


定位：

用户职业知识资产。


管理：

KnowledgeDocument
KnowledgeChunk
Embedding
KnowledgeTag


内容：

- 技术资料
- 项目文档
- 学习笔记
- 面经
- 错题
- 历史反馈


提供：

RAG 检索能力。


---

# 5.4 Interview Module


面试引擎。


核心对象：

InterviewSession
InterviewerProfile
Question
Answer
Feedback


支持四种模式。


---

## Mode 1

Role Based Interview


流程：

Resume
-
Interviewer Card
-
Interview Direction
↓
Simulation


---

## Mode 2

Knowledge Training


流程：

Knowledge Base
↓
Question Generation
↓
Answer
↓
Evaluation


---

## Mode 3

Experience Simulation


流程：

User Question Dataset
↓
AI Interview Controller
↓
Follow-up Question
↓
Evaluation


来源：

- 面经
- 历史题库
- 用户输入问题


---

## Mode 4

Interview Review


输入：

Interview Record


输出：

Technical Skill
Communication
Project Depth
Weakness
Training Plan


---

# 5.5 Calendar Module


职责：

职业相关时间管理。


核心对象：

CalendarEvent


事件类型：


Interview
Exam
Learning
Practice
Review


未来支持：

- Apple Calendar
- Google Calendar
- Feishu Calendar


---

# 5.6 Growth Module


新增模块。


负责：

用户成长数据。


核心对象：

GrowthMetric
LearningRecord
AbilityTrend
ActivityLog


数据来源：

- 学习记录
- 模拟面试评分
- 修改历史
- 完成任务


展示：

首页 Growth Widget。


---

# 5.7 AI Module


所有模型调用唯一入口。


负责：

- Provider管理
- Prompt版本
- Schema校验
- Retry
- Fallback


支持：

LLM

Embedding

Speech


禁止：

业务模块直接调用模型 SDK。


---

# 6. Data Architecture


## 核心实体关系


User
 |
 |
- CareerPipeline
 |
- Resume
 |
- KnowledgeBase
 |
- InterviewHistory
 |
- GrowthData
CareerPipeline
 |
- JobTarget
 |
- ResumeVersion
 |
- CalendarEvent
 |
- InterviewSession
InterviewSession
 |
- Question
 |
- Answer
 |
- Feedback


---

# 7. AI / RAG Architecture


流程：


Document Import
↓
Parser
↓
Chunk Split
↓
Embedding
↓
Vector Storage
↓
Retriever
↓
Context Builder
↓
LLM
↓
Structured Output
↓
Business Validation
↓
User Result


---

# 8. Data Trust Boundary


## AI 可以：

- 总结
- 分类
- 改写
- 提问
- 分析


## 程序负责：

- 数据保存
- 状态变化
- 权限
- 分数记录
- 版本管理


AI不能：

- 自动修改事实
- 自动删除数据
- 自动改变求职状态


---

# 9. Future Extension Layer


## Skill / MCP


定位：

外部 AI 调用入口。


结构：


Career OS Core
    |
    |
 Skill Layer
    |
    |
External AI Agent


能力：

- 查询简历
- 分析岗位
- 总结面试
- 制定训练计划


---

# 10. Future Agent Architecture


未来：

Career Agent
├ Job Analysis Agent
├ Resume Agent
├ Interview Coach Agent
├ Learning Planner Agent


但：

Agent 不进入核心业务层。


---

# 11. Non Goals


当前不建设：


- 自动批量投递
- 招聘市场
- 企业 ATS
- 云端团队系统
- 视频数字人面试
- 完全自动求职 Agent


---

# 12. Development Roadmap


## Phase 1

核心系统稳定：

完成：

- Desktop Shell
- Career Pipeline
- Resume Version
- Basic Interview
- Calendar


---

## Phase 2

AI能力增强：

完成：

- Knowledge Base
- RAG Interview
- Interview Review
- Growth System


---

## Phase 3

生态扩展：

完成：

- Skill/MCP
- Calendar Sync
- Career Agent
- Smart Application Assistant
