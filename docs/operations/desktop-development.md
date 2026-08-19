# 桌面开发与打包

## 当前能力

Electron 主进程负责：

- 为前端和 Spring Boot 分配随机回环端口；
- 生成每次启动独立的工作区令牌与内部运行期令牌；
- 在启动后端前保留最近 5 份 H2 冷备份；
- 启动内置 Java 运行时与 Spring Boot JAR，等待健康检查后显示窗口；
- 通过隔离 preload 提供模型密钥的保存、删除、存在性检查和运行期装载；
- 退出应用时停止本地后端。

渲染进程启用 `contextIsolation` 与 sandbox，关闭 `nodeIntegration`。后端只监听 `127.0.0.1`，H2 不开启 `AUTO_SERVER`。

## 开发启动

准备 Node.js 22、JDK 21 和 Maven，然后执行：

```bash
cd frontend
npm install
npm run desktop:start
```

该命令依次构建 Spring Boot JAR、Vue 静态资源和 Electron 主进程，再使用系统 JDK 启动本地后端。

## 当前平台开发包

```bash
cd frontend
npm run desktop:pack
```

命令使用 `jlink` 生成当前平台的精简 Java 运行时，并输出到：

```text
output/desktop/
```

macOS 和 Windows 的 Java 运行时不能跨平台生成，因此需要分别在 macOS 与 Windows 执行打包。当前配置提供 macOS app 目录和 Windows portable 目标；正式发布前仍需补充应用图标、签名、公证、Windows 实机回归和安装器策略。

## 数据和密钥目录

- H2 与附件：Electron `userData/workspace/`；
- 冷备份：`userData/workspace/backups/`；
- 加密后的模型密钥：`userData/secure/provider-keys.json`。

密钥目录不属于普通工作区备份。系统安全存储不可用时不会回退为明文文件；设置页会明确切换为“仅本次会话”，密钥只保存在主进程内存，退出后需要重新输入。用户也可以不配置 AI，继续使用本地编辑能力。

## 模型协议

设置页支持：

- OpenAI 兼容协议：OpenAI、DeepSeek、智谱 GLM、通义千问、Moonshot 与自定义兼容地址；
- Anthropic Messages；
- Google Gemini GenerateContent。

非敏感配置保存在 H2；密钥只在 Electron 主进程中解密，并通过带内部令牌的回环请求装载到后端内存。普通 API 响应不返回密钥。
