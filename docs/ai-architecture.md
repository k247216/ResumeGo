# 职达 Career OS AI 架构设计

Version: V1.0

Status: Far-term direction draft (V2, not in current development)


# 1. AI Architecture Overview


## 1.1 AI 定位


职达中的 AI 不是聊天机器人。

而是围绕用户职业资产运行的智能能力层。


AI 负责：

- 理解用户资料
- 提供分析建议
- 生成训练内容
- 模拟真实场景


AI 不负责：

- 保存核心数据
- 修改用户事实
- 决定求职状态
- 替用户做最终选择



---

# 2. Overall AI Architecture


                User

                 |

                 |

          Career OS Core

                 |

                 |

          AI Orchestration Layer


    -------------------------------

    |             |               |

 LLM Agent    RAG Engine    Evaluation Engine


    |             |               |

    --------------------------------


          Model Provider Layer


    OpenAI

    DeepSeek

    Claude

    Local Model


---

# 3. AI Module Responsibilities


## 3.1 Model Provider


统一模型接口。


负责：

- Provider管理
- API Key
- Model配置
- 请求封装


支持：

OpenAI Compatible API
DeepSeek
OpenAI
Claude
Local LLM


业务模块禁止：

直接调用模型 SDK。


---

# 3.2 Prompt Management


所有 Prompt 版本化。


例如：


Interview Prompt
v1
v2
v3


保存：

- 使用场景
- 输入结构
- 输出格式
- 修改记录



目的：

保证 AI 行为可追踪。



---

# 3.3 Structured Output Validation


AI 输出必须经过 Schema 校验。


例如：

简历优化：

AI 输出：

```json
{
 "suggestion":"",
 "reason":"",
 "evidence":"",
 "confidence":""
}

业务层验证：
- 是否引用真实证据
- 是否符合字段要求
失败：
返回降级结果。
4. Knowledge Base RAG Architecture
这是 Career OS 的核心 AI 能力。
4.1 Knowledge Flow
User Document

      |

      |

Document Parser

      |

      |

Text Chunking

      |

      |

Embedding

      |

      |

Vector Storage

      |

      |

Retriever

      |

      |

Context Builder

      |

      |

LLM

      |

      |

Structured Response

4.2 Knowledge Sources
支持：
技术资料
例如：
- Java
- Redis
- MySQL
项目资料
例如：
- 项目文档
- README
- 技术总结
面试资料
例如：
- 面经
- 真题
- 八股
用户笔记
例如：
- 学习记录
- 错题
4.3 Retrieval Strategy
检索不是简单关键词。
采用：
Semantic Search

+

Metadata Filter

+

Target Context

例如：
用户准备：
腾讯 Java 后端技术面
检索：
优先：
- Java
- 后端
- 腾讯相关面经
- 用户项目
而不是所有资料。
5. Interview Engine AI Architecture
Interview 是 AI 核心场景。
支持四种模式。
5.1 Role Based Interview
原有能力保留。
流程：
User Select

Resume Version

+

Interviewer Profile

+

Interview Direction


          |

          |

AI Interviewer


          |

          |

Conversation

输入：
- 简历
- 岗位
- 面试官角色
输出：
- 问题
- 追问
- 反馈
5.2 Knowledge Training Interview
目标：
帮助学习。
流程：
Knowledge Base

       |

       |

Question Generator

       |

       |

AI Trainer

       |

       |

Evaluation

特点：
问题来源：
用户知识资产。
例如：
用户上传 Redis 笔记。
AI：
生成：
- 基础问题
- 深入问题
- 场景问题
5.3 Experience Simulation Interview
目标：
还原真实面试。
来源：
用户提供：
- 面经
- 真题
- 历史问题
流程：
Question Dataset


      |

      |

Interview Controller


      |

      |

AI Interviewer


      |

      |

Follow-up


重要原则：
AI 不随机制造核心问题。
用户提供的问题是真实依据。
5.4 Interview Review Engine
输入：
完整面试记录。
处理：
Conversation

      |

      |

Evaluation


      |

      |

Feedback


输出：
维度：
Technical

Communication

Project Depth

Problem Solving

Job Fit

同时生成：
下一步训练建议。
6. Resume AI Architecture
Resume Analysis
输入：
Resume Version

+

Job Target

+

JD

输出：
Missing Evidence

Matching Analysis

Optimization Suggestion

原则：
AI 提建议。
用户确认。
生成新版本。
禁止：
AI直接覆盖简历。
7. Career Growth AI
未来能力。
目标：
让用户看到成长。
输入：
Interview Score

Learning Record

Resume Change

Feedback

生成：
Ability Trend

Weakness

Next Action

例如：
过去三次模拟：
项目表达：
60
72
85
8. AI Agent Architecture
未来扩展。
不进入核心业务。
结构：
Career OS Core


        |

        |


Agent Layer


        |

        |


External AI Platform


Agent 类型
Job Analysis Agent
能力：
分析 JD
提取要求
生成准备计划
Resume Agent
能力：
分析简历
提出修改建议
Interview Coach Agent
能力：
制定训练计划
分析弱点
Learning Planner Agent
能力：
安排学习路径
9. Skill / MCP Architecture
定位：
Career OS 对外能力接口。
例如：
ChatGPT
  |

  |
Career Skill
  |

  |
Career OS
  |

  |
User Data
能力：
查询：
- 简历
- 项目
- 面试记录
执行：
- 岗位分析
- 面试复盘
- 学习规划
10. AI Safety Boundary
AI 可以：
✓ 分析
✓ 总结
✓ 生成问题
✓ 提供建议
AI 不可以：
✗ 编造经历
✗ 修改事实
✗ 删除用户数据
✗ 自动改变岗位阶段
✗ 自动发送申请
11. AI Failure Handling
模型失败：
基础功能继续运行。
例如：
AI不可用：
仍然支持：
- 编辑简历
- 查看版本
- 管理目标
- 查看历史记录
12. Future Roadmap
V1
基础 AI：
- 简历分析
- 基础模拟面试
V1.5
知识增强：
- RAG
- 面经训练
- 面试复盘
V2
Agent：
- Career Agent
- Skill/MCP
- 多模型协同