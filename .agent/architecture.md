# 职达项目架构

## 1. 项目定位

职达是面向求职者的 AI 智能简历与求职助手。系统围绕一份用户简历和一个目标岗位，
形成“诊断—定向优化—模拟面试—反馈迭代”的成长闭环。

当前阶段只建设 12 天实训可交付的 MVP，不建设企业招聘端、招聘市场或在线投递平台。
详细边界见 `harness-docs/product/mvp.md`。

## 2. 技术栈（第一版建议）

为与课程 Web 方向的统一授课和验收环境保持一致，默认采用：

- 前端：Vue 3 + TypeScript + Vite + Pinia + Element Plus
- 后端：Java 21 + Spring Boot 3 + Spring Security + MyBatis-Plus
- 数据库：MySQL 8
- API 文档：OpenAPI 3 / Knife4j
- AI 接入：阿里云百炼 OpenAI 兼容接口，首个模型采用通义千问 Max
- 测试：Vitest、JUnit 5、Testcontainers、Playwright
- 部署：Docker Compose + Nginx

技术栈仍需团队和导师最终确认。确认前，不引入微服务、向量数据库、消息队列等非 MVP
必要组件。

## 3. 架构形态

采用“前后端分离 + 模块化单体”。模块边界按业务能力划分，不按页面划分：

```text
Web Client
    |
REST API
    |
Spring Boot Modular Monolith
    ├── identity       用户与认证
    ├── resume         简历、经历、版本与证据库
    ├── job            职位描述与岗位画像
    ├── assessment     简历评分（AI 禁飞区）
    ├── matching       岗位匹配排序（AI 禁飞区）
    ├── optimization   AI 修改建议、差异与采纳记录
    ├── interview      模拟面试与对话状态机（AI 禁飞区）
    ├── feedback       面试结果反哺简历
    ├── export         PDF 导出
    └── ai             模型适配、Prompt、Schema 校验与调用审计
        |
MySQL / Qwen Max
```

## 4. 核心依赖规则

- Controller 只做协议转换、参数校验和权限检查。
- Application Service 编排用例，不直接拼接 Prompt 或执行 SQL。
- Domain 保存确定性业务规则，尤其是三个 AI 禁飞区。
- Repository 隐藏持久化细节。
- `ai` 模块只能通过明确接口被业务模块调用，模型供应商 SDK 不得渗透到业务层。
- 禁飞区不得依赖大模型决定最终分数、排序结果或状态转换。
- 模块间只能通过公开接口和稳定 DTO 协作，禁止跨模块访问数据库表。

## 5. 关键数据流

```text
用户经历
  → 结构化简历 + 能力证据库
  → 简历确定性评分
  → JD 结构化
  → 确定性匹配排序与差距分析
  → AI 生成有证据来源的修改建议
  → 用户采纳后生成新简历版本
  → 状态机驱动模拟面试
  → 回答评价与能力缺口
  → 反哺证据库和下一版简历
```

## 6. AI 输出边界

- AI 输出必须使用版本化 Prompt 和结构化 JSON Schema。
- AI 负责抽取、改写、生成问题和给出解释性评价。
- 程序负责权限、状态、权重、排序、校验、持久化和最终决策。
- 所有简历改写必须关联用户提供的事实证据；证据不足时返回“需要补充”，禁止编造。
- 模型失败、超时或输出不合法时，系统必须给出可理解的降级结果。

## 7. 隐私与安全

- 简历、邮箱和手机号属于敏感信息，不写入普通日志和开发 AI 使用日志。
- 上传文件必须校验类型、大小和内容，解析结果按不可信输入处理。
- JD 和简历文本不得被当作系统指令执行，防范 Prompt Injection。
- API Key 仅通过环境变量或密钥管理注入，不得提交到仓库。
- 用户可以删除简历、面试记录和 AI 处理结果。

## 8. 可观测性

每次请求使用关联 ID。AI 调用至少记录功能类型、模型、Prompt 版本、耗时、状态、
Schema 校验结果和降级原因，但不记录原始敏感文本。

每个核心用例都应有稳定的错误码、结构化日志和自动化测试，以支持 30 分钟 Bug 注入评测。
