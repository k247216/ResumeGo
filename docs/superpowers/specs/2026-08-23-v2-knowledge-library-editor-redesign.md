# V2 Knowledge Library Editor And Layout Redesign

Status: user-approved design; implementation pending

Visual baseline: [`docs/design/v2-knowledge-library-target.png`](../../design/v2-knowledge-library-target.png)

This design refines the approved Knowledge Library interaction specification. The global black application rail remains always visible and does not gain a collapse feature. The work focuses on the Knowledge Library's internal panes, real file types, frictionless note creation, and editing the managed Markdown copy.

## Product outcome

The Knowledge Library should feel like a focused local desktop workspace rather than a web dashboard. A user can quickly create or import an asset, identify its real type, read it, edit an editable asset, and reorganize the workspace without losing the current document.

## Chosen approach

Use a managed-asset editor:

- local `NOTE` records and imported `.md` managed copies are editable;
- `.txt` and future non-editable formats remain read-only;
- editing imported Markdown changes only ResumeGo's managed copy, never the user's original path;
- the managed source file and extracted searchable content remain consistent after a successful save.

Rejected alternatives:

- converting imported Markdown into a NOTE would lose source identity and file operations;
- editing only extracted database text would make the visible content disagree with the managed source file;
- writing back to the user's original path would create surprising external mutations.

## Desktop layout

### Global application rail

- Keep the existing black global rail visible on every page.
- Do not implement rail collapse.
- Replace the Knowledge Library navigation icon with a document/library icon that visually matches the approved target and remains distinct from the Resume icon.
- Preserve current routes, theme behavior, tooltips, and user state.

### Knowledge Library panes

The page remains a desktop multi-pane workspace:

1. global application rail;
2. library navigator;
3. document list;
4. reading/editor workspace;
5. optional source inspector.

Remove the outer rounded card treatment. Use the full page canvas, alignment, white space, and one-pixel separators as in the approved target.

Recommended wide-screen widths:

| Pane | Expanded | Collapsed |
| --- | ---: | ---: |
| Library navigator | 196–208px | 0 |
| Document list | 310–324px | 0 |
| Reading/editor | `minmax(480px, 1fr)` | never collapsed |
| Source inspector | 276–288px | 0 |

Navigator, document list, and inspector must fully disappear when closed. They must not leave a 44px strip or long restore rail. Compact icon buttons in the page command bar restore the navigator and list; the existing reading-header action restores the inspector. Focus returns to the restore control after closing a pane.

Pane visibility is stored only in local UI storage. It does not sync to cloud state and does not affect selected folder, document, expanded nodes, search results, or scroll position.

At 1080×720, the navigator closes first, then the inspector; the document list stays available unless the user closes it. No mobile card layout or horizontal page overflow is allowed.

## Visual language

- Match the approved target's white/black desktop palette, restrained green status color, spacing, typography hierarchy, and continuous rows.
- Use existing icon-library assets; do not use text glyphs such as `▸`, `＋`, or `✎` as visible controls.
- Folder and document rows are continuous, with subtle hover/selection surfaces and hairline separators instead of cards.
- The reading/editor pane is the visual focus and uses a comfortable 65–80 Chinese-character line length.
- Do not display unsupported PDF, DOCX, or PPTX types merely because they appear in the long-term target image.

## Real file types

The public document response must include an explicit safe `sourceExtension` for imported files. The renderer must not infer a type from a title or invent a fallback format.

Current mapping:

| Data | Visible type | Editable |
| --- | --- | --- |
| `sourceType=NOTE` | Local note | yes |
| `sourceType=FILE`, `sourceExtension=md` | Markdown | yes |
| `sourceType=FILE`, `sourceExtension=txt` | TXT | no |
| unknown/missing extension | File | no |

The list shows a matching icon, type label, update time, category path or tag, and real processing state. Search results use the same metadata contract.

## Frictionless note creation

Clicking `New note` performs one action without opening a dialog:

1. create a real NOTE record and empty persisted content;
2. assign a server-generated ID and a local default title such as `Untitled note`;
3. place it in the currently selected library folder when possible;
4. select the new record;
5. focus the inline title editor, then allow immediate body editing.

The title and body are edited in the reading workspace. A title update uses an explicit backend operation; it is not a renderer-only optimistic rename. Failed creation leaves the previous selection unchanged. Failed folder assignment keeps the note but reports that it is unfiled.

## Note and Markdown editing

### Editable assets

`NOTE` and managed `.md` files share the same editor presentation and explicit save action. Switching documents with unsaved changes must ask the user whether to keep editing, discard, or cancel the switch. There is no silent autosave in this iteration.

### Managed Markdown save

Saving imported Markdown must:

1. confirm ownership and that the source is an available managed `.md` file;
2. validate UTF-8 content and the existing size limit;
3. prepare a temporary file inside the managed storage boundary;
4. atomically replace the managed source copy;
5. update extracted searchable content, size, hash, and document update time;
6. return the server-authoritative document and content.

If any persistence step fails, the service restores or retains the previous managed copy and database state. A hash collision with another managed asset is reported honestly and does not overwrite either document. Absolute paths, hashes, and temporary locations never enter ordinary logs or renderer responses.

`TXT` remains read-only. Its reading pane has no edit action.

## Component and contract changes

- `DesktopShell.vue`: icon replacement only; no collapse state.
- `KnowledgeCommandBar.vue`: compact pane restore controls and one-click new note.
- `KnowledgeNavigator.vue`: 0-width close behavior and icon-library controls.
- `KnowledgeDocumentList.vue`: 0-width close behavior and extension-driven icons/labels.
- `KnowledgeReadingPane.vue`: inline title editing, NOTE/Markdown editing, dirty-state guard, read-only TXT.
- `KnowledgeLibraryView.vue`: pane-state orchestration, immediate edit after creation, selection guard.
- Knowledge DTO/API: explicit safe extension, document rename, one-click NOTE creation semantics, managed Markdown content save.
- Knowledge storage service: atomic managed-copy update and metadata/content consistency.

## Error and state boundaries

- Errors stay keyed by document ID; an error from document A never appears on B.
- Closing or restoring a pane never reloads data or changes selection.
- Create, rename, and save show truthful pending, success, and failure states.
- A failed Markdown save keeps the editor dirty and preserves the last persisted reading state.
- Browser development mode may edit NOTE and Markdown through the backend, but desktop-only open/reveal actions continue to report `DESKTOP_REQUIRED`.

## Verification

Required automated coverage:

- safe extension returned for list, detail, search, retry, and create responses;
- one-click note creates a real record, selects it, and enters edit mode;
- inline title rename success and failure consistency;
- NOTE and Markdown save success, ownership isolation, size limit, hash collision, and rollback behavior;
- TXT has no edit control;
- navigator/list/inspector close to zero width and restore without losing state;
- keyboard focus and unsaved-change guard;
- all existing Knowledge tests, full frontend tests, backend tests, web build, and Electron build.

Visual QA compares the implemented page with the approved target at 1440×960 and 1080×720 in light and dark themes. The build is not complete until the real page, not a static mock, passes that comparison and its primary interactions work.

