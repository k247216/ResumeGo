# V2-F1-UX-01：Pipeline 页面交互契约

## Identity

- Status: `INTEGRATED`
- Owner: Core Controller with user approval
- Code branch: none
- Approved contract: `docs/superpowers/specs/2026-08-22-v2-pipeline-interaction-design.md`

## Product question

将旧“求职目标”页面替换为真实 Career Pipeline 工作区，同时不让它重新承担 Workspace、完整简历编辑、完整日历或完整模拟面试职责。

## Required design output

- 多 Pipeline 列表、当前 Pipeline 身份和生命周期。
- 有序阶段的查看、推进、新增、重命名、排序和历史入口。
- 公司/岗位/JD/简历版本的显式编辑和缺失关联修复状态。
- 关联日程与面试计划的摘要及进入专业页面的路径。
- 创建、空态、加载、失败、重试、归档/恢复和无效关联。
- 桌面最小尺寸、宽屏、浅色/深色和键盘操作验收截图/规格。

## Product boundaries

- Pipeline 是真实机会，不是 Knowledge 容器。
- 目标切换不自动切换或修改独立简历库。
- 页面不展示写死成长数据、AI 状态大卡或完整简历预览。
- 删除 Pipeline 不在 F1；归档必须保留历史和关联。

## Exit gate

已满足。用户授权 Core Controller 按依赖连续发放，交互契约已冻结；F1-FE-03 可以提升为 `READY`。
