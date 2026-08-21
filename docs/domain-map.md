Career OS Domain Map v1.0
0. 核心思想
整个系统不是围绕页面组织。
而是围绕：
用户职业资产（Career Assets）

组织。
核心资产：
能力事实
    ↓
知识资产
    ↓
岗位机会
    ↓
表达材料
    ↓
训练过程
    ↓
反馈成长
1. 总体领域关系图
                         User
                          |
                          |
        -----------------------------------
        |          |          |            |
        ↓          ↓          ↓            ↓


 Knowledge     Resume    Career Pipeline   Calendar

 Base          System        System        System

        |          |             |            |

        |          |             |            |

        -------- Interview Engine --------

                         |

                         |

                    Feedback

                         |

                         |

                    Growth System


2. 六大领域职责
Domain 1：Career Pipeline
目标：
管理：
我要去哪里。

核心实体：
CareerPipeline

JobTarget

Stage

ApplicationStatus

拥有：
公司

岗位

JD

阶段

关联简历

面试计划

投递状态

不拥有：
❌ 技术知识
❌ 学习资料
❌ AI聊天
❌ 简历内容
输入：
User
Resume
Calendar

输出：
Interview Context

Resume Requirement

Next Action

Domain 2：Resume System
目标：
管理：
我如何表达自己。

实体：
Resume

ResumeVersion

Evidence

ChangeHistory

关系：
Evidence

    ↓

Resume Version

    ↓

Job Target

例如：
用户事实：
完成医疗RAG项目
↓
腾讯版本：
构建三层知识图谱RAG系统
↓
字节版本：
优化检索链路，提高响应效率
输出：
提供给：
- Pipeline
- Interview
- AI
Domain 3：Knowledge Base（核心）
目标：
管理：
我拥有什么能力。

实体：
Document

Chunk

KnowledgeTag

Embedding

KnowledgeCollection

来源：
学习资料

项目文档

面试题

面经

个人总结

输出：
给：
Interview

AI Analysis

Resume

Growth

这是 Career OS 最大长期资产。
Domain 4：Interview Engine
目标：
验证：
我是否真的掌握。

实体：
InterviewSession

Question

Answer

Evaluation

Feedback

输入：
ResumeVersion

JobTarget

KnowledgeBase

InterviewerProfile

输出：
Feedback

Weakness

GrowthAction

三种模式：
Role Based
来源：
Resume + Target
Knowledge Training
来源：
Knowledge Base
Experience Simulation
来源：
Question Dataset
+
AI Controller

Domain 5：Calendar
目标：
管理：
什么时候发生。

实体：
CalendarEvent

Schedule

Reminder

事件：
Interview

Learning

Practice

Review

Exam

重要原则：
Calendar 独立。
例如：
面试事件：
可以关联：
JobTarget

但是：
不是属于 JobTarget。
原因：
未来可以同步：
系统日历
飞书
Google Calendar
Domain 6：Growth System
目标：
管理：
我有没有进步。

实体：
GrowthRecord

AbilityMetric

Trend

Achievement

来源：
Interview Feedback

Learning Record

Resume Improvement

Practice History

输出：
给：
Workspace。
3. 数据流冻结
求职流程
用户发现岗位

↓

Career Pipeline

↓

导入JD

↓

Resume匹配

↓

生成岗位版本

↓

Interview训练

↓

Feedback

↓

Growth

↓

更新Resume

学习流程
资料导入

↓

Knowledge Base

↓

学习训练

↓

Knowledge Interview

↓

Evaluation

↓

Growth

面试流程
Calendar Event

↓

Interview Session

↓

Resume + Knowledge + Target

↓

AI Interview

↓

Feedback

↓

Next Action

4. AI层位置
注意：
AI 不属于任何业务领域。
正确：
                 AI Layer


              /     |     \


 Resume AI  Interview AI  Knowledge AI


AI调用：
Domain Service

↓

AI Orchestrator

↓

Model Provider

错误：
Vue页面

↓

OpenAI API

禁止。
5. 前端页面映射
Workspace
数据来源：
Calendar

Growth

Activity

Career Pipeline summary

作用：
行动入口。
Career Pipeline 页面
结构：
Master List

+

Detail Workspace

Resume 页面
结构：
Library

+

Inspector

Knowledge 页面
结构：
Document Library

+

Knowledge Explorer

Interview 页面
结构：
Composer

+

Session Room

+

Review

Calendar 页面
结构：
Calendar

+

Inspector

6. 后端模块建议
从现在架构迁移：
目前：
project
resume
matching
optimization
interview
feedback
ai
未来：
career
 |
 ├── pipeline
 |
 ├── target


resume


knowledge


interview


calendar


growth


ai
 |
 ├── provider
 ├── prompt
 ├── orchestration

7. 一个重要决策：不要创建 AI Module 业务化
很多 AI 项目会变成：
AI聊天
AI简历
AI面试
AI学习

最后碎掉。
你的结构应该：
Career OS

业务领域

↓

AI增强

8. 最终产品核心图
                 Career OS


                      User


                       |


        --------------------------------


        Career Assets Layer


        Knowledge Base

        Resume Version

        Career Pipeline


        --------------------------------


        Action Layer


        Interview Engine

        Calendar

        Growth


        --------------------------------


        Intelligence Layer


        AI Orchestration


冻结结论
以后任何新功能必须挂靠一个领域：
例如：
“自动投递”
不能成为：
Application Center
而应该：
Career Pipeline

+

External Action Adapter

“AI职业规划”
不能成为：
AI Advisor
而应该：
Growth

+

AI Orchestration

“面试题库”
不能成为：
Question Center
而应该：
Knowledge Base

↓

Interview Engine