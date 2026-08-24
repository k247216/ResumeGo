# V2 Knowledge Library Foundation Contract

Status: Frozen for `V2-F2-BE-01`

## Slice boundary

The first Knowledge slice stores trustworthy local metadata only. It does not copy files, extract text, search, tag, delete, call AI, or claim that a source is available before bytes are safely stored.

## Ownership and objects

- Every record belongs to the current local `user_id`; all reads include that ownership filter.
- `knowledge_documents` is the user-facing library object.
- `knowledge_source_files` is a future file-copy record owned by one document. BE-01 creates the table but does not create file rows.
- Knowledge content is not automatically Resume evidence or a verified ability fact.

## Frozen schema

H2 `V6__knowledge_library_foundation.sql` and MySQL `V26__knowledge_library_foundation.sql` must be equivalent.

`knowledge_documents`:

- `id`, `user_id`, `title`, `source_type`, `processing_status`, `created_at`, `updated_at`.
- `source_type`: `NOTE` or `FILE`; BE-01 public creation accepts only `NOTE`.
- `processing_status`: `NOT_STARTED`, `PENDING`, `COMPLETED`, `FAILED`; BE-01 always creates `NOT_STARTED`.
- index `(user_id, updated_at, id)`.

`knowledge_source_files`:

- `id`, `document_id`, `user_id`, `original_name`, `stored_relative_path`, `mime_type`, `extension`, `size_bytes`, `sha256`, `availability`, `created_at`, `updated_at`.
- one source file per document; unique `(document_id)` and `(user_id, sha256)`.
- `stored_relative_path` is relative to the V2 data directory. Absolute paths and traversal segments are forbidden.
- `availability`: `STAGED`, `AVAILABLE`, `MISSING`, `QUARANTINED`.
- deleting a document eventually owns cleanup of the source and derived rows; deletion is not implemented in BE-01.

## API contract for BE-01

- `POST /api/v2/knowledge/documents` with `{ "title": string, "sourceType": "NOTE" }`.
- `GET /api/v2/knowledge/documents` returns current-user records ordered by `updated_at DESC, id DESC`.
- `GET /api/v2/knowledge/documents/{id}` returns only a current-user record.
- Response fields: `id`, `title`, `sourceType`, `processingStatus`, nullable `sourceFile`, `createdAt`, `updatedAt`.
- Title is trimmed, internal whitespace is collapsed, length is 1-120. Unsupported source types are rejected with 400; missing or foreign IDs return 404 without revealing ownership.

## Later slices

- IO-01 alone may register `FILE` documents after staging validates and copies bytes.
- BE-02 owns categories, tags and keyword search. Secure opening of managed source files is a separate Electron capability slice after search, so the renderer never receives or submits arbitrary paths.
- BE-03 owns retry and complete deletion. F2 uses no embeddings or vector database.

## Security and failure behavior

- No original name, content, path, hash or user text enters ordinary logs.
- Renderer never supplies a system path to the backend in BE-01.
- A failed create leaves no partial document or source row.
- Tests use synthetic metadata only and prove cross-user isolation.
