# V2-F0-MIG-02：V1 只读检查与事务导入引擎

## Identity

- Status: `QUEUED`，禁止开始
- Branch: `codex/v2-f0-mig-02-import-engine`
- Base commit: `Assigned when promoted to READY`
- Depends on: approved and integrated `V2-F0-MIG-01`

## User result

用户可以先检查一个 V1 备份，看见可导入数量、警告和不支持项；确认后将副本转换到 V2，失败不会污染任一工作区。

## Planned scope

- 只读 manifest/schema 检查和 fingerprint。
- dry-run API 与结构化结果。
- Job Project → Career Pipeline、JD、选定 Resume 的显式映射。
- 新 V2 transaction/temporary workspace 中导入、计数校验、原子激活和 receipt。
- 相同 fingerprint 的幂等处理。

## Scope out

- 不实现文件选择 UI、自动扫描 V1 目录、V2→V1、云同步或静默导入。
- 不修改 V1 文件、数据库或安装包。

## File ownership to freeze at promotion

仅授权新的 `com.resumego.migration.v1` 模块、对应测试和 MIG-01 明确列出的 H2/MySQL 迁移；不得修改 Pipeline/Resume 内部表写法，跨模块写入必须走冻结的 import ports。

## Required tests

正常 dry-run、未知 schema、损坏文件、跨用户/非法路径、重复 fingerprint、关联缺失、事务中途失败、计数不一致、成功 receipt，以及导入前后 V1 hash/mtime 不变。局部测试后必须运行 `cd backend && mvn test`。

## Promotion gate

MIG-01 必须给出精确 DTO、端点、表/迁移、导入 Port、staging 与原子激活方式；随后填写完整 Base commit 和 Allowed files。
