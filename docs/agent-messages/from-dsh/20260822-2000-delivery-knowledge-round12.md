# DSH 交付：PDF 灰色背景根治——按真实页面尺寸渲染（第 12 轮）

- From: DSH
- Type: delivery
- Related: 协调者反馈（PDF 显示仍有灰色容器背景）
- Date: 2026-08-22 20:00Z

## 根因

- 此前 PDF 舞台尺寸硬编码为 A4（794×1123）。当 PDF 页面不是 A4（如 US Letter 612×792pt、横版等）时，插件在 iframe 内按页面自身比例排版，四周出现**灰色留白（letterbox）**——这就是你看到的"灰色容器背景"。

## 修复

- 打开 PDF 时从文件字节解析**首页 MediaBox**（`/MediaBox [0 0 W H]`，pt → px @96dpi），舞台/iframe 基础尺寸 = 真实页面尺寸；解析失败回退 A4。
- 舞台、视口、预览区全部显式纯白背景；`ensureMediaBase` 不再覆盖已检测到的尺寸。

## 实测

- 横版 PDF（792×612pt）：基础 1056×816（横版比例 1.29），自动适应 50% 按宽度约束——无灰边。
- US Letter（612×792pt）：基础 816×1056，适应 65%。
- A4（595×842pt）：基础 794×1123。

## 验证

- 前端全量 307 通过、`npm run build` 通过；后端无改动。
- 截图 docs/audits/round27-knowledge-light-{1440x960,1080x720}.png。

请核查。