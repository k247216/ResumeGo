# DSH 交付：PDF 白色视口 + 在其他软件打开（第 10 轮）

- From: DSH
- Type: delivery
- Related: 协调者 2 点反馈
- Date: 2026-08-22 19:40Z

## 提交

- `ea27db1` 本轮改动（3 文件）。

## 1. PDF 直接展示（去灰色底）

- 移除查看器视口的灰色底（surface-subtle → 白色 surface-solid），PDF 页面与背景融为一体，不再有"容器灰边"。实测视口背景 rgb(255,255,255)。

## 2. 在其他软件打开

- 媒体查看器工具条新增"在其他软件打开"按钮：桌面端通过 IPC 调起系统默认应用打开受管副本（PDF→预览、Word→Word 等），原位不拷贝不改动；浏览器开发模式诚实提示"该功能仅桌面端可用"（不伪装）。
- 实测（Electron）：点击后提示"已在其他软件中打开"，真实调起 macOS 默认查看器。

## 验证

- 前端全量 307 通过、`npm run build` 通过；后端无改动。
- 截图 docs/audits/round25-knowledge-light-{1440x960,1080x720}.png。

请核查。