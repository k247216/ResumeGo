# 职达 Career OS 数据模型设计

Version: V1.0

Status: Far-term direction draft (V2, not in current development)


# 1. 数据设计原则


## 1.1 用户资产优先

系统围绕用户长期积累的数据设计。

核心资产：

- 求职目标
- 简历版本
- 项目经历
- 职业知识
- 面试记录
- 成长记录


这些资产不会因为一次求职结束而失效。


---

## 1.2 历史不可覆盖


重要数据采用版本化。


包括：

- Resume Version
- Interview Record
- Feedback
- Growth History


禁止：

直接覆盖历史状态。



# 2. Entity Overview


User
 |
 |
- CareerPipeline
 |
- Resume
 |
- KnowledgeBase
 |
- InterviewSession
 |
- CalendarEvent
 |
- GrowthRecord


---

# 3. User


用户实体。


字段：

id
name
avatar
createdAt
settings


职责：

拥有所有个人职业资产。



---

# 4. Career Domain


## CareerPipeline


表示一次求职机会集合。


例如：

腾讯 Java 后端。


字段：

id
userId
name
status
createdAt
updatedAt


状态：

ACTIVE
PAUSED
SUCCESS
ARCHIVED


---

## JobTarget


具体岗位。


字段：

id
pipelineId
company
position
jdContent
source
stage
createdAt


例如：

腾讯
Java 后端实习
技术面阶段


---

## PipelineStage


阶段定义。


默认：

Preparation
OnlineTest
TechnicalInterview
HRInterview
Offer


支持未来：

用户自定义阶段。


---

# 5. Resume Domain


## Resume


基础简历。


字段：

id
userId
name
createdAt


例如：

张三技术简历


---

## ResumeVersion


岗位版本。


字段：

id
resumeId
targetId
version
content
snapshot
createdAt


关系：

Resume
 |
- ResumeVersion


例如：

基础简历
 |
腾讯Java V3
字节后端 V2


---

## Evidence


能力证据。


字段：

id
resumeVersionId
type
content
source


来源：

Project
Internship
Competition
Course


作用：

支撑 AI 修改。


---

# 6. Knowledge Domain


## KnowledgeDocument


用户上传资料。


字段：

id
userId
name
type
path
createdAt


类型：

PDF
Markdown
TXT
URL


---

## KnowledgeChunk


切片。


字段：

id
documentId
content
embeddingId
metadata


用于：

RAG。


---

## KnowledgeTag


分类。


例如：

Java
Redis
项目
面经


---

# 7. Interview Domain


## InterviewSession


一次模拟面试。


字段：

id
userId
targetId
resumeVersionId
mode
status
createdAt


Mode：


ROLE_BASED
KNOWLEDGE_BASED
EXPERIENCE_BASED


---

## InterviewerProfile


面试官。


字段：

id
name
role
focus
description


例如：

高级Java工程师
关注：
JVM
并发
系统设计


---

## InterviewQuestion


问题。


字段：

id
sessionId
content
source
order


source：

AI_GENERATED
USER_IMPORTED
KNOWLEDGE_BASE


---

## InterviewAnswer


回答。


字段：

id
questionId
content
duration


---

## FeedbackRecord


反馈。


字段：

id
sessionId
score
analysis
suggestions
createdAt


注意：

分数不是绝对能力。

只是一次训练反馈。


---

# 8. Calendar Domain


## CalendarEvent


职业事件。


字段：

id
userId
type
title
startTime
endTime
relatedTargetId


类型：

INTERVIEW
EXAM
LEARNING
PRACTICE
REVIEW


注意：

Workspace Upcoming 不直接绑定 Target。

Calendar 是独立时间来源。


---

# 9. Growth Domain


## GrowthRecord


成长记录。


字段：

id
userId
type
value
source
createdAt


例如：

模拟面试评分
学习时间
完成训练


---

## AbilityTrend


能力趋势。


字段：

id
userId
dimension
score
time


维度：

Technical
Communication
Project


---

# 10. 数据关系总结


User
 |
- CareerPipeline
     |
  - JobTarget
     |
     + ResumeVersion
    
     |
     + InterviewSession
    
     |
     + CalendarEvent
User
 |
- Resume
 |
- KnowledgeBase
 |
- GrowthRecord


InterviewSession
 |
- Question
 |
- Answer
 |
- Feedback


---

# 11. 数据安全原则


- 默认本地存储
- 用户主动控制导入
- API Key安全保存
- 删除数据必须彻底
- AI调用范围透明