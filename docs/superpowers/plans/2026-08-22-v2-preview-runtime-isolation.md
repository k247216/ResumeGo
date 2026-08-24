# V2 Preview Runtime Isolation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the V2 preview a separately installable desktop application whose runtime data directory cannot overlap the V1 ResumeGo directory.

**Architecture:** A small Electron identity module owns the V2 preview runtime name and resolves its `userData` path from Electron's platform-specific `appData` root. The main process applies that path before any workspace, log, secure-key or backend process is initialized. Packaging metadata uses a distinct application id and display name.

**Tech Stack:** Electron 37, TypeScript 6, Vitest 4, electron-builder 26

**Spec:** `docs/superpowers/specs/2026-08-22-career-os-v2-foundation-design.md`

## Global Constraints

- V1 application id remains `com.resumego.desktop`; V2 preview uses `com.resumego.careeros.preview`.
- V2 preview display name is `职达 Career OS Preview`.
- V2 preview data directory name is `ZhidaCareerOSPreview` and must not equal `ResumeGo`.
- The data path is set before creating workspace, backup, log or secure-key paths.
- No V1 database or backup is opened or modified by this slice.
- Node.js 22, Java 21 and the existing Electron security settings remain unchanged.

---

### Task 1: Define and verify the V2 runtime identity

**Files:**
- Create: `frontend/electron/productIdentity.ts`
- Create: `frontend/electron/productIdentity.test.ts`

**Interfaces:**
- Consumes: an absolute Electron `appData` root string.
- Produces: `V2_PREVIEW_IDENTITY` and `resolveV2PreviewUserDataPath(appDataRoot: string): string`.

- [ ] **Step 1: Write the failing identity tests**

```ts
import { describe, expect, it } from 'vitest'
import { V2_PREVIEW_IDENTITY, resolveV2PreviewUserDataPath } from './productIdentity'

describe('V2 preview product identity', () => {
  it('resolves a userData directory that is isolated from V1', () => {
    expect(resolveV2PreviewUserDataPath('/Users/test/Library/Application Support'))
      .toBe('/Users/test/Library/Application Support/ZhidaCareerOSPreview')
    expect(resolveV2PreviewUserDataPath('/Users/test/Library/Application Support'))
      .not.toBe('/Users/test/Library/Application Support/ResumeGo')
  })

  it('exposes the approved preview display identity', () => {
    expect(V2_PREVIEW_IDENTITY.appName).toBe('ZhidaCareerOSPreview')
    expect(V2_PREVIEW_IDENTITY.productName).toBe('职达 Career OS Preview')
    expect(V2_PREVIEW_IDENTITY.appId).toBe('com.resumego.careeros.preview')
  })
})
```

- [ ] **Step 2: Run the focused test and verify RED**

Run: `cd frontend && npm test -- electron/productIdentity.test.ts`

Expected: FAIL because `./productIdentity` does not exist.

- [ ] **Step 3: Implement the minimal identity module**

```ts
import path from 'node:path'

export const V2_PREVIEW_IDENTITY = Object.freeze({
  appName: 'ZhidaCareerOSPreview',
  productName: '职达 Career OS Preview',
  appId: 'com.resumego.careeros.preview',
  userDataDirectoryName: 'ZhidaCareerOSPreview',
})

export function resolveV2PreviewUserDataPath(appDataRoot: string): string {
  return path.join(appDataRoot, V2_PREVIEW_IDENTITY.userDataDirectoryName)
}
```

- [ ] **Step 4: Run the focused test and verify GREEN**

Run: `cd frontend && npm test -- electron/productIdentity.test.ts`

Expected: 2 tests pass.

- [ ] **Step 5: Commit the identity module**

```bash
git add frontend/electron/productIdentity.ts frontend/electron/productIdentity.test.ts
git commit -m "feat(desktop): define V2 preview identity"
```

### Task 2: Apply the isolated path before desktop initialization

**Files:**
- Modify: `frontend/electron/main.ts`
- Create: `frontend/electron/productIdentityIntegration.test.ts`

**Interfaces:**
- Consumes: Electron `app.getPath('appData')` before `app.whenReady()`.
- Produces: Electron `userData` set to the V2 preview directory before `startApplication()` derives workspace, secure-store and log paths.

- [ ] **Step 1: Write the failing integration contract test**

The test reads the compiled Electron main bundle because the observable safety contract is the startup side effect and Electron itself cannot be started inside the unit-test process.

```ts
import { readFileSync } from 'node:fs'
import path from 'node:path'
import { describe, expect, it } from 'vitest'

describe('V2 preview desktop startup', () => {
  it('sets an isolated userData path before application startup', () => {
    const main = readFileSync(path.resolve('electron-dist/main.js'), 'utf8')
    const setPath = main.indexOf("setPath('userData'")
    const start = main.lastIndexOf('startApplication()')
    expect(setPath).toBeGreaterThan(-1)
    expect(start).toBeGreaterThan(setPath)
    expect(main).toContain('resolveV2PreviewUserDataPath')
  })
})
```

- [ ] **Step 2: Build Electron, run the focused test and verify RED**

Run: `cd frontend && npm run build:electron && npm test -- electron/productIdentityIntegration.test.ts`

Expected: FAIL because the compiled main process does not call `app.setPath('userData', ...)`.

- [ ] **Step 3: Apply the V2 identity in the main process**

Import the identity module, then execute the following before IPC handlers or application startup derive local paths:

```ts
app.setName(V2_PREVIEW_IDENTITY.appName)
app.setPath('userData', resolveV2PreviewUserDataPath(app.getPath('appData')))
```

Do not change the workspace, secure-key, backup or log implementations; they will inherit the isolated `userData` root.

- [ ] **Step 4: Build Electron, run both identity test files and verify GREEN**

Run: `cd frontend && npm run build:electron && npm test -- electron/productIdentity.test.ts electron/productIdentityIntegration.test.ts`

Expected: 3 tests pass.

- [ ] **Step 5: Commit startup isolation**

```bash
git add frontend/electron/main.ts frontend/electron/productIdentityIntegration.test.ts
git commit -m "feat(desktop): isolate V2 preview data"
```

### Task 3: Separate the packaged application identity

**Files:**
- Modify: `frontend/package.json`
- Modify: `frontend/package-lock.json`

**Interfaces:**
- Consumes: electron-builder package metadata.
- Produces: application id `com.resumego.careeros.preview`, display name `职达 Career OS Preview`, package name `zhida-career-os-preview`, version `2.0.0-preview.1`.

- [ ] **Step 1: Update packaging metadata**

Set:

```json
{
  "name": "zhida-career-os-preview",
  "version": "2.0.0-preview.1",
  "description": "本地优先的个人职业资产与求职成长操作系统",
  "build": {
    "appId": "com.resumego.careeros.preview",
    "productName": "职达 Career OS Preview"
  }
}
```

Run `npm install --package-lock-only --ignore-scripts` so the lockfile root metadata matches without upgrading dependencies.

- [ ] **Step 2: Verify package metadata and desktop compilation**

Run: `cd frontend && node -e "const p=require('./package.json'); if(p.name!=='zhida-career-os-preview'||p.version!=='2.0.0-preview.1'||p.build.appId!=='com.resumego.careeros.preview'||p.build.productName!=='职达 Career OS Preview') process.exit(1)"`

Run: `cd frontend && npm run build:electron`

Expected: both commands exit 0.

- [ ] **Step 3: Run the complete frontend verification**

Run: `cd frontend && npm test && npm run build`

Expected: all frontend tests pass and Vite production build succeeds.

- [ ] **Step 4: Commit packaging isolation**

```bash
git add frontend/package.json frontend/package-lock.json
git commit -m "chore(desktop): separate V2 preview package"
```

### Task 4: Verify repository and document the delivered boundary

**Files:**
- Modify: `docs/product/roadmap.md`

**Interfaces:**
- Consumes: completed runtime and package verification results.
- Produces: an evidence-backed V2-F0 progress entry.

- [ ] **Step 1: Run full regression verification**

Run: `cd backend && mvn test`

Run: `cd frontend && npm run build:electron && npm test && npm run build`

Expected: backend and frontend test suites pass; frontend production build succeeds.

- [ ] **Step 2: Update the F0 roadmap status**

Record only verified facts: separate package identity, separate runtime data directory, test counts and remaining migration-preflight work. Do not mark all of F0 complete until the V1 importer exists.

- [ ] **Step 3: Check the final diff**

Run: `git diff --check && git status --short`

Expected: no whitespace errors; only planned V2 files are modified.

- [ ] **Step 4: Commit the verified progress record**

```bash
git add docs/product/roadmap.md
git commit -m "docs(v2): record preview isolation progress"
```
