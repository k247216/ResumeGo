# V2-F2-BE-01：Knowledge 元数据基础

## Identity

- Status: `QUEUED`，禁止开始
- Branch: `codex/v2-f2-be-01-knowledge-metadata`
- Base commit: `Assigned when promoted to READY`
- Depends on: approved and integrated F2-ARCH-01

## User result

用户能够创建一条本地知识文档记录、查看列表与详情，并看见真实来源类型、文件状态和处理状态；尚未导入或解析时不会伪造内容。

## Planned scope

- H2/MySQL 等价迁移。
- 新 `com.resumego.knowledge` domain/repository/service/controller/dto 模块。
- 创建元数据、列表和详情 API；当前用户所有权、时间和状态映射。
- 空库、缺失记录和不可用关联的稳定错误。

## Scope out

不复制文件、不解析、不搜索、不打标签、不删除、不调用 AI。

## File ownership to freeze

ARCH-01 指定的两份迁移、新 Knowledge 模块和对应 repository/service/controller 测试。禁止修改 V1 Evidence、Resume、Pipeline 表和全局异常契约。

## Required tests

空库、创建/list/detail、规范化、重复标识、当前用户隔离、H2 migration、MySQL migration 静态一致性、无正文日志。真实 RED/GREEN 后运行 `cd backend && mvn test`。

