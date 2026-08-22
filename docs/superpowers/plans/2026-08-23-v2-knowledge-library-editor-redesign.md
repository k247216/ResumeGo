# V2 Knowledge Library Editor And Layout Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make Knowledge Library a target-faithful desktop workspace with real file types, one-click editable notes, editable managed Markdown copies, and internal panes that fully close without changing the global application rail.

**Architecture:** Extend the public Knowledge document contract with a safe extension and an explicit title update. Keep NOTE content in the database, but route imported Markdown saves through a dedicated managed-copy editor that atomically replaces the file and updates searchable content and metadata. The Vue view keeps business state in Pinia and owns only local pane visibility, dirty-selection guards, and focus restoration.

**Tech Stack:** Java 21, Spring Boot 3.5, JdbcTemplate, Flyway-managed H2/MySQL schemas, Vue 3, TypeScript, Pinia, Vitest, Vue Test Utils, Element Plus icons, Electron TypeScript build.

**Spec:** `docs/superpowers/specs/2026-08-23-v2-knowledge-library-editor-redesign.md`

## Global Constraints

- The black global application rail remains visible; do not add a global rail collapse feature.
- Only NOTE and managed `.md` assets are editable; TXT and unknown formats are read-only.
- Markdown edits change only ResumeGo's managed copy and never the user's original location.
- Do not expose absolute paths, hashes, staging paths, or tokens to the renderer or ordinary logs.
- Navigator, document list, and source inspector close to zero width and retain selection and scroll state.
- Use icon-library components for visible controls; do not use text glyphs such as `▸`, `＋`, or `✎`.
- Do not add PDF, DOCX, PPTX, RAG, AI chat, knowledge graph, or directory monitoring.
- Preserve per-document error isolation and user ownership filtering.

---

### Task 1: Real Extension Contract And File-Type Presentation

**Files:**
- Modify: `backend/src/main/java/com/resumego/knowledge/dto/KnowledgeDocumentResponse.java`
- Modify: `backend/src/main/java/com/resumego/knowledge/KnowledgeSearchRow.java`
- Modify: `backend/src/main/java/com/resumego/knowledge/KnowledgeRepository.java`
- Modify: `backend/src/main/java/com/resumego/knowledge/KnowledgeService.java`
- Modify: `backend/src/main/java/com/resumego/knowledge/KnowledgeRecoveryService.java`
- Modify: `backend/src/main/java/com/resumego/knowledge/KnowledgeClassificationService.java`
- Modify: `backend/src/test/java/com/resumego/knowledge/KnowledgeServiceTest.java`
- Modify: `backend/src/test/java/com/resumego/knowledge/KnowledgeClassificationServiceTest.java`
- Modify: `backend/src/test/java/com/resumego/knowledge/KnowledgeControllerTest.java`
- Modify: `frontend/src/types/knowledge.ts`
- Modify: `frontend/src/components/knowledge/KnowledgeDocumentList.vue`
- Modify: `frontend/src/components/knowledge/KnowledgeDocumentList.test.ts`

**Interfaces:**
- Produces: `KnowledgeDocumentResponse.sourceExtension(): String | null` and `KnowledgeDocument.sourceExtension: string | null`.
- Consumes: existing `KnowledgeSourceFile.extension()` and `KnowledgeSearchRow` SQL projection.

- [ ] **Step 1: Write failing backend contract tests**

Add assertions that NOTE returns `null`, Markdown returns `"md"`, TXT returns `"txt"`, and search/retry responses preserve the extension:

```java
assertThat(service.get(markdownId).sourceExtension()).isEqualTo("md");
assertThat(service.get(noteId).sourceExtension()).isNull();
assertThat(search.search("Redis", null, null, true).getFirst()
        .document().sourceExtension()).isEqualTo("md");
```

- [ ] **Step 2: Run the backend tests and verify RED**

Run:

```bash
cd backend
mvn -q -Dtest=KnowledgeServiceTest,KnowledgeClassificationServiceTest,KnowledgeControllerTest test
```

Expected: compilation or assertion failure because `sourceExtension` is absent.

- [ ] **Step 3: Add the explicit safe extension to every response path**

Add `String sourceExtension` after `sourceFile` in `KnowledgeDocumentResponse`. Add `String sourceExtension` to `KnowledgeSearchRow`, select `sf.extension AS source_extension`, and map it. NOTE returns `null`; FILE uses the owned source record:

```java
KnowledgeSourceFile source = SOURCE_NOTE.equals(doc.sourceType()) ? null
        : repository.findSourceFileByDocument(userId(), doc.id()).orElse(null);
return new KnowledgeDocumentResponse(
        doc.id(), doc.title(), doc.sourceType(), doc.processingStatus(),
        source == null ? null : source.originalName(),
        source == null ? null : source.extension(),
        doc.createdAt().toString(), doc.updatedAt().toString());
```

- [ ] **Step 4: Write and run the frontend file-type tests**

Extend `KnowledgeDocument` with `sourceExtension`. Test the visible labels and data attributes:

```ts
const md = { ...doc(1), sourceExtension: 'md', sourceFile: 'redis.md' }
const txt = { ...doc(2), sourceExtension: 'txt', sourceFile: 'redis.txt' }
expect(wrapper.get('[data-test="doc-row-1"]').text()).toContain('Markdown')
expect(wrapper.get('[data-test="doc-row-2"]').text()).toContain('TXT')
```

Use `sourceExtension`, not title parsing, in `typeLabel()` and `iconOf()`.

Run:

```bash
cd frontend
npm test -- --run src/components/knowledge/KnowledgeDocumentList.test.ts src/api/knowledge.test.ts
```

Expected: PASS.

- [ ] **Step 5: Commit the contract slice**

```bash
git add backend/src/main/java/com/resumego/knowledge backend/src/test/java/com/resumego/knowledge frontend/src/types/knowledge.ts frontend/src/components/knowledge/KnowledgeDocumentList.vue frontend/src/components/knowledge/KnowledgeDocumentList.test.ts frontend/src/api/knowledge.test.ts
git commit -m "feat(knowledge): expose real source extensions"
```

---

### Task 2: One-Click Note Creation And Inline Rename

**Files:**
- Create: `backend/src/main/java/com/resumego/knowledge/dto/UpdateKnowledgeDocumentTitleRequest.java`
- Modify: `backend/src/main/java/com/resumego/knowledge/KnowledgeController.java`
- Modify: `backend/src/main/java/com/resumego/knowledge/KnowledgeService.java`
- Modify: `backend/src/main/java/com/resumego/knowledge/KnowledgeRepository.java`
- Modify: `backend/src/test/java/com/resumego/knowledge/KnowledgeServiceTest.java`
- Modify: `backend/src/test/java/com/resumego/knowledge/KnowledgeControllerTest.java`
- Modify: `backend/src/test/java/com/resumego/knowledge/KnowledgeNoteContentIntegrationTest.java`
- Modify: `frontend/src/api/knowledge.ts`
- Modify: `frontend/src/api/knowledge.test.ts`
- Modify: `frontend/src/stores/knowledge.ts`
- Modify: `frontend/src/stores/knowledge.test.ts`
- Modify: `frontend/src/views/knowledge/KnowledgeLibraryView.vue`
- Modify: `frontend/src/views/knowledge/KnowledgeLibraryView.test.ts`
- Modify: `frontend/src/components/knowledge/KnowledgeReadingPane.vue`
- Modify: `frontend/src/components/knowledge/KnowledgeReadingPane.test.ts`
- Delete: `frontend/src/components/knowledge/KnowledgeNoteDialog.vue`

**Interfaces:**
- Produces: `PATCH /api/v2/knowledge/documents/{id}` with `{ "title": string }` returning `KnowledgeDocumentResponse`.
- Produces: `store.createUntitledNote(): Promise<KnowledgeDocument>` and `store.renameDocument(id, title): Promise<KnowledgeDocument>`.
- Produces: `KnowledgeReadingPane.beginEdit(options?: { focusTitle?: boolean }): Promise<void>`.

- [ ] **Step 1: Write failing backend create-and-rename tests**

Test that create persists empty content and returns COMPLETED, rename is owner-scoped, and invalid titles leave the old title unchanged:

```java
KnowledgeDocumentResponse created = service.create(
        new CreateKnowledgeDocumentRequest("未命名笔记", "NOTE"));
assertThat(created.processingStatus()).isEqualTo("COMPLETED");
assertThat(service.getContent(created.id()).content()).isEmpty();

KnowledgeDocumentResponse renamed = service.rename(created.id(), "Redis 复习");
assertThat(renamed.title()).isEqualTo("Redis 复习");
```

- [ ] **Step 2: Run backend tests and verify RED**

```bash
cd backend
mvn -q -Dtest=KnowledgeServiceTest,KnowledgeControllerTest,KnowledgeNoteContentIntegrationTest test
```

Expected: FAIL because rename and empty persisted content on create are absent.

- [ ] **Step 3: Implement transactional empty-note creation and owner-scoped rename**

Within the existing create transaction, call `repository.saveNoteContent(id, userId(), "")` before returning. Add:

```java
public KnowledgeDocumentResponse rename(long documentId, String rawTitle) {
    String title = normalizeTitle(rawTitle);
    if (repository.updateDocumentTitle(userId(), documentId, title) != 1) {
        throw new NoSuchElementException("知识文档不存在");
    }
    return findResponse(documentId);
}
```

Repository SQL must include both ID and owner:

```sql
UPDATE knowledge_documents
SET title = ?, updated_at = CURRENT_TIMESTAMP
WHERE id = ? AND user_id = ?
```

- [ ] **Step 4: Write failing frontend one-click and rename tests**

Test no note dialog is rendered, the API receives `未命名笔记`, the returned ID becomes selected, and the reading pane enters title/body editing:

```ts
await wrapper.get('[data-test="knowledge-command-create-note"]').trigger('click')
await flushPromises()
expect(store.createUntitledNote).toHaveBeenCalledOnce()
expect(wrapper.find('[data-test="knowledge-note-dialog"]').exists()).toBe(false)
expect(wrapper.get('[data-test="knowledge-title-input"]').exists()).toBe(true)
```

- [ ] **Step 5: Implement the client/store/editor flow**

Add `renameKnowledgeDocument()` and make `createUntitledNote()` return the server document. In the view:

```ts
async function handleCreateNote() {
  const created = await store.createUntitledNote()
  if (selectedFolderId.value != null) {
    await store.categorizeCreatedDocument(created.id, selectedFolderId.value)
  }
  await nextTick()
  await readingPane.value?.beginEdit({ focusTitle: true })
}
```

Remove `KnowledgeNoteDialog` from the view and repository. The reading pane uses an inline title input and emits `rename-title` only after explicit confirmation or blur with a changed valid title.

- [ ] **Step 6: Run the focused frontend suite**

```bash
cd frontend
npm test -- --run src/api/knowledge.test.ts src/stores/knowledge.test.ts src/views/knowledge/KnowledgeLibraryView.test.ts src/components/knowledge/KnowledgeReadingPane.test.ts
```

Expected: PASS.

- [ ] **Step 7: Commit the note workflow**

```bash
git add backend/src/main/java/com/resumego/knowledge backend/src/test/java/com/resumego/knowledge frontend/src/api/knowledge.ts frontend/src/api/knowledge.test.ts frontend/src/stores/knowledge.ts frontend/src/stores/knowledge.test.ts frontend/src/views/knowledge frontend/src/components/knowledge/KnowledgeReadingPane.vue frontend/src/components/knowledge/KnowledgeReadingPane.test.ts frontend/src/components/knowledge/KnowledgeNoteDialog.vue
git commit -m "feat(knowledge): create and edit notes inline"
```

---

### Task 3: Managed Markdown Copy Editing

**Files:**
- Create: `backend/src/main/java/com/resumego/knowledge/KnowledgeManagedContentService.java`
- Modify: `backend/src/main/java/com/resumego/knowledge/KnowledgeController.java`
- Modify: `backend/src/main/java/com/resumego/knowledge/KnowledgeFileStore.java`
- Modify: `backend/src/main/java/com/resumego/knowledge/KnowledgeRepository.java`
- Modify: `backend/src/main/java/com/resumego/knowledge/dto/SaveKnowledgeNoteContentRequest.java`
- Create: `backend/src/test/java/com/resumego/knowledge/KnowledgeManagedContentServiceTest.java`
- Modify: `backend/src/test/java/com/resumego/knowledge/KnowledgeNoteContentIntegrationTest.java`
- Modify: `backend/src/test/java/com/resumego/knowledge/KnowledgeFileStoreTest.java`
- Modify: `frontend/src/api/knowledge.ts`
- Modify: `frontend/src/stores/knowledge.ts`
- Modify: `frontend/src/stores/knowledge.test.ts`
- Modify: `frontend/src/components/knowledge/KnowledgeReadingPane.vue`
- Modify: `frontend/src/components/knowledge/KnowledgeReadingPane.test.ts`

**Interfaces:**
- Consumes: `PUT /api/v2/knowledge/documents/{id}/content` and the explicit `sourceExtension` contract.
- Produces: `KnowledgeManagedContentService.save(documentId, content): KnowledgeContentResponse`.
- Produces: `KnowledgeFileStore.replaceManagedWithRollback(...)` helpers confined to the V2 data root.

- [ ] **Step 1: Write failing service and file-store tests**

Cover NOTE success, MD success, TXT rejection, foreign document 404, unavailable source rejection, size limit, duplicate hash collision, database failure restoration, and path escape rejection:

```java
assertThat(service.save(markdownId, "# 新正文").content()).isEqualTo("# 新正文");
assertThat(Files.readString(managedPath)).isEqualTo("# 新正文");
assertThat(repository.findExtractedContentByDocument(1L, markdownId).orElseThrow().content())
        .isEqualTo("# 新正文");

assertThatThrownBy(() -> service.save(txtId, "changed"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Markdown");
```

- [ ] **Step 2: Run the managed-content tests and verify RED**

```bash
cd backend
mvn -q -Dtest=KnowledgeManagedContentServiceTest,KnowledgeFileStoreTest,KnowledgeNoteContentIntegrationTest test
```

Expected: compilation failure because the managed editor does not exist.

- [ ] **Step 3: Implement safe file replacement primitives**

Add methods that resolve only the current user's managed source, create a backup in `knowledge/staging`, replace with `ATOMIC_MOVE` plus `REPLACE_EXISTING`, restore on failure, and delete temporary files. Do not accept renderer paths.

```java
public ManagedReplacement prepareReplacement(long userId, String storedRelativePath, byte[] content) {
    Path target = resolveOwnedSource(userId, storedRelativePath);
    Path replacement = stage(content);
    Path backup = stage(Files.readAllBytes(target));
    return new ManagedReplacement(target, replacement, backup);
}
```

- [ ] **Step 4: Implement database metadata/content update and collision guard**

Add one owner-scoped repository operation that updates extracted content, `size_bytes`, `sha256`, availability, and both timestamps. Before file replacement, reject `findSourceFileBySha(userId, newHash)` when it belongs to another document.

Use `TransactionTemplate` in `KnowledgeManagedContentService` so a database exception is caught before returning. On exception, restore the backup; on success, delete it. Content is never logged.

- [ ] **Step 5: Route the existing content endpoint through the managed editor**

`KnowledgeController.saveContent()` calls `managedContent.save(id, request.content())`. The service branches by real server data:

```java
if ("NOTE".equals(document.sourceType())) return saveNote(document, content);
if ("FILE".equals(document.sourceType()) && "md".equals(source.extension())) {
    return saveMarkdown(document, source, content);
}
throw new IllegalStateException("仅本地笔记和 Markdown 受管副本支持编辑");
```

- [ ] **Step 6: Enable editing only for NOTE and real Markdown in Vue**

Replace the NOTE-only condition with:

```ts
const editable = computed(() => props.document?.sourceType === 'NOTE'
  || (props.document?.sourceType === 'FILE' && props.document.sourceExtension === 'md'))
```

Assert TXT has no `[data-test="knowledge-edit-start"]`, while MD does. Reuse the same explicit save button and per-document error state.

- [ ] **Step 7: Run backend and frontend focused verification**

```bash
cd backend
mvn -q -Dtest=KnowledgeManagedContentServiceTest,KnowledgeFileStoreTest,KnowledgeNoteContentIntegrationTest,KnowledgeControllerTest test
cd ../frontend
npm test -- --run src/stores/knowledge.test.ts src/components/knowledge/KnowledgeReadingPane.test.ts
```

Expected: PASS.

- [ ] **Step 8: Commit managed Markdown editing**

```bash
git add backend/src/main/java/com/resumego/knowledge backend/src/test/java/com/resumego/knowledge frontend/src/api/knowledge.ts frontend/src/stores/knowledge.ts frontend/src/stores/knowledge.test.ts frontend/src/components/knowledge/KnowledgeReadingPane.vue frontend/src/components/knowledge/KnowledgeReadingPane.test.ts
git commit -m "feat(knowledge): edit managed markdown copies"
```

---

### Task 4: Dirty-State Selection Guard

**Files:**
- Create: `frontend/src/components/knowledge/KnowledgeUnsavedDialog.vue`
- Create: `frontend/src/components/knowledge/KnowledgeUnsavedDialog.test.ts`
- Modify: `frontend/src/components/knowledge/KnowledgeReadingPane.vue`
- Modify: `frontend/src/components/knowledge/KnowledgeReadingPane.test.ts`
- Modify: `frontend/src/views/knowledge/KnowledgeLibraryView.vue`
- Modify: `frontend/src/views/knowledge/KnowledgeLibraryView.test.ts`

**Interfaces:**
- Produces: `KnowledgeReadingPane.hasUnsavedChanges(): boolean` and `discardChanges(): void`.
- Produces: dialog events `keep-editing`, `discard`, and `save`.

- [ ] **Step 1: Write failing dirty-switch tests**

```ts
await wrapper.get('[data-test="knowledge-edit-start"]').trigger('click')
await wrapper.get('[data-test="knowledge-body-editor"]').setValue('未保存')
await wrapper.get('[data-test="doc-row-2"]').trigger('click')
expect(store.select).not.toHaveBeenCalledWith(2)
expect(wrapper.get('[data-test="knowledge-unsaved-dialog"]').exists()).toBe(true)
```

Test discard selects the pending document, keep-editing preserves document 1, and save waits for successful persistence before selecting document 2.

- [ ] **Step 2: Run tests and verify RED**

```bash
cd frontend
npm test -- --run src/views/knowledge/KnowledgeLibraryView.test.ts src/components/knowledge/KnowledgeReadingPane.test.ts src/components/knowledge/KnowledgeUnsavedDialog.test.ts
```

Expected: FAIL because selection is currently immediate.

- [ ] **Step 3: Implement the explicit unsaved dialog and pending selection**

Keep `pendingDocumentId` in the view. Never use a native browser confirm. Clear the pending ID on cancel; discard editor state before selecting; on save, await the store save and proceed only after success.

- [ ] **Step 4: Run the focused frontend tests**

Run the command from Step 2. Expected: PASS.

- [ ] **Step 5: Commit the selection guard**

```bash
git add frontend/src/components/knowledge/KnowledgeUnsavedDialog.vue frontend/src/components/knowledge/KnowledgeUnsavedDialog.test.ts frontend/src/components/knowledge/KnowledgeReadingPane.vue frontend/src/components/knowledge/KnowledgeReadingPane.test.ts frontend/src/views/knowledge/KnowledgeLibraryView.vue frontend/src/views/knowledge/KnowledgeLibraryView.test.ts
git commit -m "feat(knowledge): protect unsaved editor changes"
```

---

### Task 5: Target-Faithful Pane Layout And Collapse Behavior

**Files:**
- Modify: `frontend/src/layouts/DesktopShell.vue`
- Modify: `frontend/src/layouts/DesktopShell.test.ts`
- Modify: `frontend/src/views/knowledge/KnowledgeLibraryView.vue`
- Modify: `frontend/src/views/knowledge/KnowledgeLibraryView.test.ts`
- Modify: `frontend/src/components/knowledge/KnowledgeCommandBar.vue`
- Modify: `frontend/src/components/knowledge/KnowledgeCommandBar.test.ts`
- Modify: `frontend/src/components/knowledge/KnowledgeNavigator.vue`
- Modify: `frontend/src/components/knowledge/KnowledgeFolderNode.vue`
- Modify: `frontend/src/components/knowledge/KnowledgeDocumentList.vue`
- Modify: `frontend/src/components/knowledge/KnowledgeDocumentList.test.ts`
- Modify: `frontend/src/components/knowledge/KnowledgeReadingPane.vue`
- Modify: `frontend/src/components/knowledge/KnowledgeSourceInspector.vue`

**Interfaces:**
- Produces: local keys `resumego:knowledge:navigator-open`, `resumego:knowledge:list-open`, and `resumego:knowledge:inspector-open`.
- Produces: command-bar events `restore-navigator` and `restore-list`.
- Preserves: global application rail always visible and all existing route names.

- [ ] **Step 1: Write failing layout and collapse tests**

Assert the global rail remains, the Knowledge icon uses the chosen document/library icon, closed internal panes are absent rather than 44px strips, restore controls appear, and selection is unchanged:

```ts
await wrapper.get('[data-test="navigator-collapse"]').trigger('click')
expect(wrapper.find('[data-test="knowledge-navigator"]').exists()).toBe(false)
expect(wrapper.get('[data-test="restore-knowledge-navigator"]').exists()).toBe(true)
expect(store.selectedDocumentId).toBe(1)
```

At 1080px, navigator and inspector default closed; at 1440px they respect the user's stored preference instead of reopening on every resize.

- [ ] **Step 2: Run frontend tests and verify RED**

```bash
cd frontend
npm test -- --run src/layouts/DesktopShell.test.ts src/views/knowledge/KnowledgeLibraryView.test.ts src/components/knowledge/KnowledgeCommandBar.test.ts src/components/knowledge/KnowledgeDocumentList.test.ts
```

Expected: FAIL because collapsed components currently leave restore rails and resize overwrites preferences.

- [ ] **Step 3: Implement zero-width pane orchestration**

The parent conditionally renders panes:

```vue
<KnowledgeNavigator v-if="navigatorOpen" @close="closeNavigator" />
<KnowledgeDocumentList v-if="listOpen" @close="closeList" />
<KnowledgeSourceInspector v-if="inspectorOpen && store.selectedDocument" />
```

Command-bar restore buttons render only for closed panes. Store explicit user choices locally; responsive defaults apply only when no stored choice exists.

- [ ] **Step 4: Match the approved desktop visual language**

Remove `.library-body` outer border/radius/card background. Use 1px pane separators, 196–208px navigator, 310–324px list, 276–288px inspector, continuous document rows, and a 65–80-character reading measure. Replace `▸`, `＋`, and `✎` with Element Plus icons such as `Expand`, `Plus`, `EditPen`, `Delete`, `Folder`, `Document`, and `MoreFilled`.

Keep black/white as the primary palette and use green only for real success/selection/action state.

- [ ] **Step 5: Run focused tests and production type/build checks**

```bash
cd frontend
npm test -- --run src/layouts/DesktopShell.test.ts src/views/knowledge/KnowledgeLibraryView.test.ts src/components/knowledge
npm run build
npm run build:electron
```

Expected: all commands exit 0.

- [ ] **Step 6: Commit the visual and pane slice**

```bash
git add frontend/src/layouts/DesktopShell.vue frontend/src/layouts/DesktopShell.test.ts frontend/src/views/knowledge frontend/src/components/knowledge
git commit -m "feat(knowledge): align desktop library workspace"
```

---

### Task 6: Integrated Functional And Visual Acceptance

**Files:**
- Modify: `design-qa.md`
- Modify: `docs/design-qa.md` only if repository convention requires the accepted visual record there.

**Interfaces:**
- Consumes: completed Tasks 1–5.
- Produces: verified Knowledge Library at `/knowledge` and a design QA record with `final result: passed`.

- [ ] **Step 1: Run complete automated verification**

```bash
cd backend
mvn test
cd ../frontend
npm test
npm run build
npm run build:electron
```

Expected: backend and frontend tests report zero failures; both builds exit 0.

- [ ] **Step 2: Start the integrated local application**

Run the repository's documented V2 backend and frontend development commands on non-conflicting local ports. Use only fictional local fixtures created for QA; never commit the generated database.

- [ ] **Step 3: Verify the real user flow**

Complete this sequence in the app:

1. create a note with one click and confirm immediate title/body editing;
2. save, switch documents, restart, and confirm persistence;
3. import `.md` and `.txt`, verify distinct types and editability;
4. edit Markdown and verify the managed copy and search results update;
5. close and restore navigator, list, and inspector without changing selection;
6. trigger a failed save on document A and confirm document B shows no leaked error;
7. verify browser open/reveal actions report the desktop requirement honestly.

- [ ] **Step 4: Capture and compare the required viewports**

Capture the approved target and implemented page in the same state at 1440×960 and 1080×720, light and dark. Record mismatches for spacing, pane widths, icons, borders, typography, clipping, and focus visibility in `design-qa.md`.

- [ ] **Step 5: Fix P0–P2 mismatches and repeat comparison**

Repeat functional capture and comparison until `design-qa.md` contains:

```text
final result: passed
```

P3 polish may remain as explicitly listed follow-up notes.

- [ ] **Step 6: Commit acceptance evidence**

```bash
git add design-qa.md docs/design-qa.md
git commit -m "test(knowledge): verify editor workspace acceptance"
```

