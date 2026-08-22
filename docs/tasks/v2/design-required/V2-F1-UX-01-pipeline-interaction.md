# V2-F1-UX-01：Pipeline 页面交互契约

## Identity

- Status: `DESIGN_REQUIRED`
- Owner: Core Controller with user approval
- Code branch: none

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

用户批准信息层级和关键交互后，记录组件边界、route、data-test 和响应式规则，才能将 F1-FE-03 升为 `READY`。
