# V2-F2-QA-01：Knowledge F2 纵向验收

## Identity

- Status: `QUEUED`，禁止开始
- Branch: `codex/v2-f2-qa-01-knowledge-acceptance`
- Base commit: `Assigned when promoted to READY`
- Depends on: integrated F2-FE-02

## Outcome

证明 Knowledge Base 第一版是可信、可管理、可检索的本地资料库，并可以作为后续 F3 的稳定资产层。

## Required scenarios

- 空 workspace 导入每种批准格式，重启后文档、分类、标签和文本仍可用。
- 重复文件、伪造类型、超限、损坏、复制失败和解析失败具有真实状态。
- 中文/英文关键词搜索返回可追溯片段，无结果不生成内容。
- 打开原文只允许受管文件；任意路径和跨用户 token 被拒绝。
- retryable job 可重试，应用中断后可以恢复。
- 删除影响摘要准确；确认删除后文件及全部派生内容不可搜索，其他文档不受影响。
- 日志、数据库测试材料和交付报告不包含真实源正文或密钥。

## Planned Allowed files

仅新增 F2 集成测试、桌面 smoke 测试/记录与虚构 fixture。发现缺陷必须退回对应 BE/IO/FE 任务，不在 QA 分支修改生产实现。

## Verification

运行后端/前端全量、Vite/Electron build、空工作区、重启、备份恢复和目标平台包 smoke。按 `docs/testing/v2-final-test-matrix.md` 记录，所有 F3/RAG 项继续标为 `NOT_RUN_UNTIL_IMPLEMENTED`。
