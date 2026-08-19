# ResumeGo

> 职达——本地优先、证据驱动的 AI 求职工作台。

ResumeGo 面向正在准备实习、校招或初级岗位的求职者。它围绕一个真实目标岗位，帮助用户整理能力证据、维护简历版本、获得可追溯的修改建议、进行模拟面试，并把反馈带回下一轮准备。

## 当前状态

项目正在从 Web 原型重置为可长期维护的个人桌面产品。现有 Vue 与 Spring Boot 功能作为能力草本保留，但产品结构、交互、数据可信度和桌面运行方式将逐步重做。

当前代码仍是 Web 架构，尚不能作为无需配置的桌面应用交付。真实状态和主要问题见 [当前基线](docs/baseline.md)。

## 产品原则

- 以一个真实求职目标为主线，不堆砌彼此割裂的功能入口。
- 用户经历是事实来源；AI 不得编造经历、技能、数字或荣誉。
- AI 建议必须说明依据，并由用户决定是否采纳。
- 简历修改产生新版本，不静默覆盖原内容。
- 简历、联系方式和面试内容默认保存在本地。
- 即使 AI 暂时不可用，用户仍能查看、编辑、导出和删除自己的数据。

## 技术概览

- 前端：Vue 3、TypeScript、Vite、Element Plus
- 后端：Java 21、Spring Boot、MyBatis-Plus、Flyway
- 当前数据库：MySQL
- 目标桌面容器：Electron
- 目标本地存储：H2 文件数据库

桌面化采用渐进式迁移：优先复用现有 Vue 和 Spring Boot 能力，再逐步处理本地存储、进程管理、系统集成、安装包和更新。

## 文档

- [当前基线](docs/baseline.md)
- [产品定义](docs/product.md)
- [产品路线图](docs/roadmap.md)
- [架构说明](docs/architecture.md)
- [开发与完成标准](docs/development.md)
- [隐私与 AI 边界](docs/privacy-and-ai.md)
- [关键决策](docs/decisions.md)

## 开发验证

前端：

```bash
cd frontend
npm install
npm run build
```

后端：

```bash
cd backend
mvn test
```

本仓库仍处于产品重置阶段。公开发布前需要完成桌面打包、隐私检查、数据迁移和安装包验证。
