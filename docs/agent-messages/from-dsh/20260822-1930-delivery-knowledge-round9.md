# DSH 交付：PDF 单层渲染预览（与 PNG 一致）（第 9 轮）

- From: DSH
- Type: delivery
- Related: 协调者反馈（PDF 预览像 PNG 一样，不要两层缩放）
- Date: 2026-08-22 19:30Z

## 提交

- `4265b69` 本轮改动（3 文件）。

## 改动

- PDF 预览改为**单层渲染**：iframe 固定基础分辨率（794×1123），插件只渲染一次；缩放纯 CSS `transform: scale`（66%→116% 平滑，不触发插件重渲染）。
- 舞台（stage）布局尺寸 = 基础 × 缩放，滚动区与页面一致（拖拽/滚动正确、放大不丢图）。
- 与 PNG 图片预览完全一致的交互：− / 百分比 / ＋ / 适应 + 拖拽平移 + Ctrl+滚轮。

## 实测

- 适应 66%：iframe 布局恒为 794×1123、transform scale(0.66)、舞台 524px、无横向溢出（宽高同时适配）。
- 放大 116%：iframe 布局不变（单次渲染）、transform scale(1.16)、舞台/滚动区 921px。

## 验证

- 前端全量 307 通过、`npm run build` 通过；后端无改动。
- 截图 docs/audits/round24-knowledge-light-{1440x960,1080x720}.png。

请核查。