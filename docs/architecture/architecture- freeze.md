职达 Career OS Architecture Freeze v1.0
0. 冻结声明
项目：
ResumeGo（内部工程名）
产品：
职达 Career OS
定位：
本地优先的 AI 职业成长操作系统。

核心目标：
帮助用户围绕职业目标建立个人职业资产，通过简历、知识库、模拟面试和成长反馈形成持续提升闭环。
1. 顶层产品结构冻结
一级模块固定为：
Career OS

├── Workspace
│
├── Career Pipeline
│
├── Resume
│
├── Interview Engine
│
├── Knowledge Base
│
├── Calendar
│
├── Growth
│
└── Settings

禁止：
未来新增一级入口：
例如：
❌ AI助手
❌ 职业聊天
❌ 投递中心
❌ 学习中心
原因：
这些应该作为现有模块中的能力，而不是新的产品孤岛。
2. 核心领域模型冻结
用户资产模型
User

 |
 |
 ├── Career Pipeline
 |
 ├── Resume System
 |
 ├── Knowledge Base
 |
 ├── Interview History
 |
 ├── Calendar
 |
 └── Growth Data

3. Career Pipeline 冻结
定位
管理：
“我要去哪里”。
不是：
“我要怎么提升”。
核心对象
CareerPipeline

JobTarget

PipelineStage

示例
腾讯 Java 后端

准备

↓

笔试

↓

技术面

↓

HR

↓

Offer

Pipeline 负责：
✅ 公司
✅ 岗位
✅ JD
✅ 当前阶段
✅ 关联简历版本
✅ 关联面试记录
✅ 关联日程
Pipeline 不负责：
❌ 学习资料
❌ 技术知识
❌ 通用能力训练
❌ AI聊天
这些属于 Knowledge / Interview。
4. Resume System 冻结
核心思想：
不是“一份简历”。
而是：
基础职业资产 + 岗位表达版本。

结构：
Resume Base

      |

      |

Resume Version


腾讯 Java V3

字节后端 V2

阿里 Java V1

Resume负责：
✅ 内容管理
✅ Markdown/PDF
✅ 版本
✅ 差异
✅ 证据引用
Resume不负责：
❌ 面试训练
❌ 岗位管理
❌ 学习记录
5. Knowledge Base 冻结
这是未来核心壁垒。
定位：
用户职业知识资产。
结构：
Knowledge Base

├ 技术知识

├ 项目资料

├ 面试题

├ 面经

├ 学习笔记

└ 历史反馈

负责：
✅ 文档导入
✅ 分类
✅ RAG
✅ 检索
不负责：
❌ 简历展示
❌ 岗位阶段
❌ 面试流程
6. Interview Engine 冻结
定位：
能力训练系统。
不是聊天窗口。
固定四种模式：
Mode 1
Role Based Interview
保留当前功能。
流程：
Resume Version

+

Interviewer Profile

+

Direction

↓

AI Interview

Mode 2
Knowledge Training
流程：
Knowledge Base

↓

Question

↓

Answer

↓

Evaluation

用途：
学习。
Mode 3
Experience Simulation
流程：
真实面经/题库

↓

AI控制流程

↓

追问

↓

评价

用途：
模拟真实面试。
Mode 4
Interview Review
流程：
Conversation

↓

Analysis

↓

Feedback

↓

Training Action

7. Workspace 冻结
这是之前争论最多的部分，现在明确。
首页不是：
❌ 当前岗位详情
❌ 当前目标控制台
❌ 功能展示页
首页定位：
今日职业工作台。

回答：
“现在我要做什么？”
固定包含：
Upcoming
时间提醒。
来源：
Calendar。
注意：
不绑定 Pipeline。
Quick Actions
固定：
- 导入简历
- 创建目标
- 添加日程
- 上传资料
- 开始模拟面试
Growth Widget
展示：
- 学习投入
- 面试能力趋势
- 连续准备时间
Activity Feed
展示：
最近行为。
8. Calendar 冻结
定位：
时间系统。
不是：
面试管理器。
事件：
Interview

Exam

Learning

Practice

Review

Calendar 可以关联：
JobTarget。
但是：
Calendar 本身独立存在。
9. AI 架构冻结
AI只能作为能力层。
结构：
Business Domain

       |

AI Orchestration

       |

Model Provider

禁止：
业务模块直接调用模型。
AI负责：
✅ 分析
✅ 总结
✅ 生成问题
✅ 提供建议
AI禁止：
❌ 编造经历
❌ 修改事实
❌ 改变状态
❌ 自动提交操作
10. 数据流冻结
完整闭环：
Job Target

↓

JD理解

↓

Resume Version

↓

Knowledge Preparation

↓

Interview Training

↓

Feedback

↓

Growth

↓

Next Action

11. UI空间模型冻结
全应用统一：
Master Detail
或者：
Workspace Composer
对应：
页面	模型
首页	Workspace
目标	Master Detail
简历	Library + Inspector
日历	Calendar + Inspector
设置	Settings + Inspector
面试	Composer + Inspector


禁止：
❌ 卡片堆叠 Dashboard
❌ SaaS营销布局
❌ 页面无限滚动
❌ 同一信息重复展示
12. 暂不建设冻结
以下明确排除：
自动投递
状态：
Future Exploration
原因：
不是核心价值。
招聘市场
不做。
云端协作
不做。
数字人面试
不做。
13. 开发约束冻结
以后新增功能必须回答：
Q1
是否增强职业成长闭环？
Q2
是否产生用户长期资产？
Q3
是否降低用户完成任务成本？
三个都不能满足：
不进入。
14. 当前开发优先级冻结
P0
必须完成：
1. Career Pipeline 完整化
2. Resume Version 完整化
3. Interview Engine 基础闭环
4. Knowledge Base 第一版
P1
增强：
5. Growth System
6. 面试复盘
7. RAG训练
P2
探索：
8. Skill/MCP
9. Calendar Sync
10. Career Agent
P3
未来：
11. 智能投递辅助
冻结后的项目一句话
以后所有 AI 修改前必须理解：
职达不是一个帮助用户生成简历的 AI 工具，而是一个围绕个人职业资产，通过目标管理、知识沉淀、面试训练和成长反馈帮助用户提升求职竞争力的 Career OS。


v1.1

核心定位（确认版）
职达不是一个 AI 求职工具，而是一个围绕个人职业资产，通过目标管理、知识沉淀、面试训练和反馈成长，帮助用户持续提升职业竞争力的 Career OS。

核心价值排序：
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
其中：
Knowledge Base 是长期壁垒。
其他模块都是围绕它产生价值。
一、首页 Workspace（重新定义）
定位：
不是 Dashboard。
不是成长中心。
不是目标详情页。
它是：
用户每天打开软件时的行动入口。

回答：
“我现在最应该做什么？”
权重排序：
Primary（核心）
今日行动
例如：
14:30

腾讯技术面


距离开始 2小时

[开始模拟面试]
或者：
今天没有安排

下一步：

完成 Redis 知识训练

[开始训练]
Secondary（辅助）
最近活动
例如：
昨天

完成一次模拟面试

更新腾讯简历 V3

上传 JVM 笔记
作用：
增加产品生命感。
Tertiary（弱展示）
成长反馈
不是大卡片。
不是 KPI。
类似：
成长记录

模拟面试趋势
65 → 72 → 81

本周学习
6.5h

连续准备
12天
位置：
侧边或底部。
目的：
鼓励用户。
不是管理用户。
所以首页比例：
行动
70%

活动
20%

成长
10%

这是最终方向。
二、Career Pipeline（核心业务对象）
这里确认：
你说的不是“求职项目”。
而是：
岗位求职管线。
这是一个非常重要的区别。
用户可能同时：
Career Pipeline


腾讯 Java 后端

准备中
 |
笔试
 |
技术面


----------------


字节 后端开发

准备中
 |
一面


----------------


美团 Java

准备中

每条 Pipeline：
独立拥有：
公司

岗位

JD

当前阶段

关联简历版本

面试记录

日程

反馈

下一步行动

但是：
它不是知识容器。
例如：
Java 基础知识：
属于：
Knowledge Base
不是腾讯 Pipeline。
这是非常关键的边界。
三、Knowledge Base（未来核心）
这一点我调整权重：
之前我说五大模块之一。
现在确认：
它应该是：
Career OS 的核心资产层。
原因：
Pipeline 会结束。
Offer 后这个岗位消失。
但是：
知识资产长期存在。
结构：
Knowledge Base


技术知识

    Java
    Redis
    JVM


项目知识

    医疗RAG
    Web项目


面试资产

    腾讯面经
    八股


个人总结

    学习笔记
    错题


它服务：
1. 学习
例如：
“帮我复习 Redis”
2. 面试
例如：
“基于我的 Redis 笔记考我”
3. 求职
例如：
“针对腾讯岗位提取重点”
四、Resume System
最终定位：
职业表达层。
关系：
Knowledge Base

      ↓

能力事实

      ↓

Resume Version

      ↓

岗位表达

这里保留之前确认：
简历不能直接绑定多个目标。
正确模型：
基础简历资产

      |

      |

岗位版本


腾讯Java V3


字节后端 V2

每个岗位版本：
独立演进。
这样避免：
改腾讯简历导致字节版本污染。
五、Interview Engine（三模式冻结）
确认保留三种。
Mode 1
Role Based Interview
来源：
岗位

+

简历

+

面试官角色

例如：
高级 Java 工程师。
特点：
开放训练。
Mode 2
Knowledge Training Interview
来源：
Knowledge Base

例如：
选择：
Redis
AI：
生成：
基础问题
深入问题
场景问题
目的：
学习。
Mode 3
Experience Simulation Interview
来源：
真实面经

用户题库

历史问题

重点：
不是 AI 自己创造面试。
而是：
AI 扮演面试主持人。
流程：
真实问题

↓

AI提问

↓

用户回答

↓

AI追问

↓

评价

六、Growth System
调整：
不是独立核心模块。
而是：
横向反馈层。
它服务：
Workspace。
来源：
Interview Feedback

Learning Record

Resume Improvement

生成：
能力趋势

薄弱点

下一步建议

不做：
游戏化打分。
不做：
排行榜。
七、自动投递定位
确认：
保留。
但是：
外围能力。
架构位置：
Career Pipeline


       ↓


External Action Layer


       ↓


投递辅助

未来可以：
记录：
- 投递记录
- 状态
- 平台来源
甚至：
辅助填写。
但是：
不成为核心竞争力。
八、最终产品闭环（确认版）
              Knowledge Base
                    |
                    |
                    ↓

用户能力资产

                    |
                    ↓

            Career Pipeline

                    |
                    ↓

           Resume Version

                    |
                    ↓

          Interview Engine

                    |
                    ↓

             Feedback

                    |
                    ↓

            Growth System

                    |
                    ↓

          Workspace Next Action

                    |
                    ↓

             下一轮提升

九、现在冻结后的优先级
我认为你的开发顺序应该调整：
P0（现在）
1. Career Pipeline 完整化
因为它是所有流程入口。
需要：
- 多岗位
- 阶段
- 状态
- 关联关系
2. Knowledge Base 第一版
这是最大壁垒。
先不要追求复杂 Agent。
完成：
- 文件导入
- 分类
- 搜索
- 基础 RAG
3. Interview Engine 重构
基于：
Pipeline + Resume + Knowledge。
P1
Resume Version 完善
Growth Feedback
Calendar Sync
P2
Skill/MCP
Agent
投递辅助