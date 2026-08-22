# V2-F2-IO-01：文件导入与可恢复解析

## Identity

- Status: `QUEUED`，禁止开始
- Branch: `codex/v2-f2-io-01-import-extraction`
- Base commit: `Assigned when promoted to READY`
- Depends on: integrated F2-BE-01

## User result

用户明确选择本地文件后，V2 将安全副本保存到自己的数据目录并异步提取文本；失败会展示真实状态并可恢复，不损坏原文件和其他知识资产。

## Planned scope

- Electron 文件选择与受限 IPC capability。
- 文件 fingerprint、类型/大小校验和安全复制。
- import/extraction job 的状态机、重启恢复和结构化失败类别。
- 首批批准格式的确定性文本解析；保留原始文件与提取内容分离。
- API 返回 processing 状态，不把长文本写入日志。

## Scope out

不做 OCR、网页 URL、文件夹监控、云盘、Embedding/RAG 或自动标签。

## Ownership boundary

升为 READY 时必须将 Electron capability、Knowledge import service、parser、job repository、API 和各自测试精确分文件。renderer 不得获得任意 filesystem 或 shell 权限。

## Required tests

支持/不支持类型、伪造扩展名、超限、重复 fingerprint、路径穿越、复制中断、解析失败、重启恢复、重试候选状态、源文件不变、日志无正文。运行后端和前端/Electron受影响测试及 build。

