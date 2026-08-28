# 职达 Career OS

[![Release](https://img.shields.io/github/v/release/k247216/ResumeRefineHelper?display_name=tag&sort=semver)](https://github.com/k247216/ResumeRefineHelper/releases)
[![License](https://img.shields.io/github/license/k247216/ResumeRefineHelper)](LICENSE)
[![Vue](https://img.shields.io/badge/Vue-3-42b883?logo=vuedotjs&logoColor=white)](https://vuejs.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6db33f?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Electron](https://img.shields.io/badge/Electron-37-47848f?logo=electron&logoColor=white)](https://www.electronjs.org/)

> 一个本地优先的个人职业资产操作系统：把知识、求职管线、简历版本和面试反馈连接成下一步行动。

职达（ResumeGo）不是一次性生成简历的 AI 工具。它围绕用户自己的职业事实建立长期资产，在本地管理岗位机会、知识资料、简历表达和面试训练，让每一次练习都能沉淀为下一次更好的准备。

## v2.0 做了什么

- **Workspace 工作台**：打开软件即可看到下一场真实安排、当前目标、关联简历、最近复盘和下一步行动。
- **Career Pipeline 求职管线**：同时维护多家公司/岗位，独立记录阶段、JD、简历版本、面试和日程。
- **Knowledge Base 知识库**：导入和编辑本地 Markdown、PDF、TXT，支持目录、标签、搜索、预览和真实面经格式解析。
- **Resume System 简历库**：基础简历与岗位表达分离，版本不可变、可对比、可归档，并可明确绑定求职目标。
- **Interview Engine 面试引擎**：自由面试、知识训练、真题演练三种来源清晰的模式，保存开始时的上下文快照并生成复盘。
- **本地 AI 配置**：支持 OpenAI 兼容协议、Anthropic Messages、Gemini，以及 DeepSeek、GLM、通义千问等常用服务。
- **桌面运行**：Electron + 内置 Spring Boot/H2 工作区，数据默认保存在自己的电脑上。

## 产品闭环

```text
Knowledge Base
      ↓
职业事实与能力资产
      ↓
Career Pipeline → Resume Version → Interview Engine
                                      ↓
                               Feedback / Review
                                      ↓
                                 Workspace Action
```

知识资产是长期积累层；求职管线、简历版本和面试记录都保持独立。岗位切换不会偷偷切换简历，简历更新也不会改写已经完成的面试历史；面试开始时会保存当时使用的岗位、简历、资料或题集快照。

## 隐私和 AI 边界

- 简历、联系方式、面试回答、知识原文和本地数据库默认不上传。
- API Key 只通过桌面端的本地安全存储进入后端运行期，不进入普通日志、快照或导出文件。
- AI 不编造用户经历、技能、数字或真实面经；没有足够资料时会明确提示证据不足。
- AI 建议不会静默覆盖简历，用户可以查看依据、建议稿并决定是否采纳。
- 没有配置 AI 时，知识整理、简历编辑、导入、导出和历史浏览仍可使用；面试启动会给出设置入口。

## 快速开始（开发）

环境要求：Node.js 22、JDK 21、Maven。

```bash
git clone https://github.com/k247216/ResumeRefineHelper.git
cd ResumeRefineHelper/frontend
npm install
npm run dev
```

打开 Vite 输出的本地地址即可使用 Web 开发模式。需要完整桌面链路时：

```bash
cd frontend
npm run desktop:start
```

首次使用请在左侧“设置”中配置一个模型服务。密钥只保存在本机；配置完成后再进入面试房间即可调用真实回答和评价。

## 验证和打包

```bash
# 前端单元测试与生产构建
cd frontend
npm test -- --run
npm run build

# 后端测试
cd ../backend
mvn test

# 当前操作系统的 Electron 开发包
cd ../frontend
npm run desktop:pack
```

打包产物写入 `output/desktop/`。macOS 与 Windows 的精简 Java 运行时不能跨平台生成，请在对应系统分别运行打包命令；Windows portable 包使用：

```bash
npm run desktop:pack:win
```

## 文档入口

- [当前架构基线](docs/architecture/baseline.md)
- [产品范围与非目标](docs/product/product.md)
- [V2 路线图](docs/product/roadmap.md)
- [模块架构](docs/architecture/architecture.md)
- [隐私与 AI 规则](docs/architecture/privacy-and-ai.md)
- [桌面开发与打包](docs/operations/desktop-development.md)
- [关键产品决策](docs/decisions.md)
- [界面 QA 记录](docs/design-qa.md)

## 当前状态

v2.0 是职达 Career OS 的产品化开发线，重点完成了求职管线、知识库、简历库、三模式面试引擎和本地桌面运行链路。语音面试、日历同步、Skill/MCP、Agent 和投递辅助属于后续阶段，不会伪装成当前已完成能力。

欢迎通过 [Issue](https://github.com/k247216/ResumeRefineHelper/issues) 反馈真实使用中的问题或提出改进建议。涉及用户数据的改动，请优先说明数据归属、删除方式和离线降级行为。

## 项目标签

`career-os` `resume` `interview-practice` `knowledge-base` `vue3` `spring-boot` `electron` `local-first` `ai`

## License

项目使用 [MIT License](LICENSE)。第三方依赖各自的许可证以其发布信息为准。
