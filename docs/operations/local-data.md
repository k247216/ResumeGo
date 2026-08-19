# 本地数据运行、备份与恢复

ResumeGo 的桌面本地模式使用 H2 文件数据库。Spring Boot 仅监听 `127.0.0.1`，数据库路径由 `RESUMEGO_DB_PATH` 指定；未指定时，开发环境写入 `./data/resumego.mv.db`。

## 数据库兼容策略

- 现有 MySQL 安装继续使用 `db/migration` 中已经发布的迁移，不改写历史。
- 新的桌面安装使用 `db/migration-h2` 中的干净基线，不导入课程演示简历、岗位或能力证据。
- MySQL 的 `JSON` 在 H2 基线中保存为文本，由现有 Java 结构化校验负责读写边界；不把数据库 JSON 函数当作业务校验。
- MySQL 的 `UNSIGNED`、`ON UPDATE CURRENT_TIMESTAMP`、`JSON_TYPE` 和表级字符集声明不进入 H2 基线；应用服务负责更新时间，通用约束和外键仍保留。
- H2 与 MySQL 使用各自的 Flyway 历史，不允许同一个数据库在两组迁移之间切换。

## 开发启动

```bash
cd backend
SPRING_PROFILES_ACTIVE=local \
RESUMEGO_DB_PATH=/absolute/path/to/resumego \
mvn spring-boot:run
```

路径不含 `.mv.db` 后缀。首次启动只创建内部的 `Local User` 身份和面试官预设，不创建任何个人简历、岗位或证据。

## 冷备份样例

备份必须在后端进程完全退出后执行。一个工作区当前至少包含：

```text
resumego.mv.db
attachments/    # 后续引入附件时一并备份
```

关闭应用后，将整个数据目录复制到带时间戳的新目录。不要只在应用运行时复制 `.mv.db` 文件，也不要覆盖最近一份可用备份。

恢复时：

1. 确认 ResumeGo 和本地后端均已退出；
2. 先把当前数据目录另存为故障现场副本；
3. 将备份中的 `resumego.mv.db` 和附件目录复制回原位置；
4. 启动相同或更新版本的 ResumeGo，让 Flyway 只执行尚未应用的迁移；
5. 确认简历库和求职目标可读取后，再删除故障现场副本。

Electron 阶段会在启动后端前执行同样的冷备份原则：升级前先保留原始数据副本，迁移失败时停止启动并提供恢复入口，不覆盖备份。
