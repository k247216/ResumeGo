# V2-F2-BE-04：层级资料库分类树

## Identity

- Status: `INTEGRATED`
- Branch: `codex/v2-f2-be-04-hierarchical-library`
- Base commit: `d701ddc87aafc1a4f3d71043c4108c87fd8814d0`
- Depends on: integrated F2-IO-02

## User result

用户可以把 Knowledge 文档放进最多五级的职业资料文件夹，展开父文件夹时能查看所有后代资料；标签仍保持独立横向属性。

## Frozen domain contract

- 复用 `knowledge_categories` 作为职业资料文件夹，不映射用户磁盘目录。
- H2 V10 / MySQL V30 为 `knowledge_categories` 增加 nullable self-FK `parent_id`；现有分类全部成为根节点。
- 一个文档仍只能关联一个分类；标签模型不变。
- 最大深度五级；禁止自身父级、后代作为父级和任何循环。
- 分类名称继续执行当前 trim/空白归一化与 1–40 字符限制；第一版保留同一用户全局名称唯一，避免迁移期改变现有唯一约束。
- 删除仅允许没有子分类且没有直属文档的节点，否则返回 `CATEGORY_NOT_EMPTY`；不得 cascade 删除或静默移动文档。
- 移动节点后整棵子树深度不得超过五级。
- 列表返回当前用户的扁平节点：`id/name/normalizedName/parentId/depth/directDocumentCount/descendantDocumentCount/createdAt/updatedAt`，前端构树；计数由真实关联计算。
- 选择父节点搜索时通过新增可选 `includeDescendants=true` 返回后代文档；缺省 false 保持现有调用兼容。

## API changes

- `POST /api/v2/knowledge/categories` 接受 `{ name, parentId }`，`parentId` 可为 null；旧 `{ name }` 等价根节点。
- `PUT /api/v2/knowledge/categories/{id}` 使用完整请求 `{ name, parentId }`，两个字段必须显式出现，`parentId` 可为 null。
- `DELETE /api/v2/knowledge/categories/{id}` 只删除空叶节点。
- `GET /api/v2/knowledge/categories` 使用新增响应字段。
- `GET /api/v2/knowledge/search` 新增 `includeDescendants`，只有 categoryId 非空时生效。

## Scope and allowed files

- V10/V30 migration 与 migration consistency tests。
- `backend/src/main/java/com/resumego/knowledge/**` 和对应 Knowledge tests。
- `backend/src/test/resources/sql/knowledge_schema.sql`。

不得修改前端、Electron、标签层级、文档多分类、磁盘目录、默认示例数据、RAG 或关系图。

## Required functional scenarios

1. 创建根节点和三级子节点，列表 parent/depth/count 正确。
2. 现有分类迁移后均为根节点且原文档关联不变。
3. 两个用户可以各自创建树，父级读取、移动、删除均隔离。
4. 拒绝跨用户 parent、自循环、后代循环和超过五级的创建/移动。
5. 非空叶节点和含子节点分类删除返回 409，不改变任何数据。
6. 父分类 includeDescendants 搜索包含后代文档；false 只返回直属文档。
7. 移动节点后计数与搜索立即一致；标签筛选仍可叠加。
8. 任一失败事务不留下半移动或错误深度。

## Verification

先运行新增 repository/service/controller/integration tests，再运行一次 `mvn -q -Dtest='Knowledge*Test' test`；只在交付前运行一次后端全量和 H2/MySQL migration diff-check。

## Direct rejection

- 前端静态构造层级或写入默认“技术知识”等示例。
- 接受任意磁盘路径作为分类。
- cascade 删除子树或其中资料。
- 标签也变成树、文档支持多个文件夹或顺带实现拖拽 UI。

## Delivery

Commit: `feat(knowledge): add hierarchical library categories`

回传最终 commit、迁移兼容性、循环/深度算法、计数口径和必要测试；不得自行合并。
