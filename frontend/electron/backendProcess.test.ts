// @vitest-environment node

import path from 'node:path'
import { describe, expect, it } from 'vitest'
import { buildBackendLaunchSpec } from './backendProcess.js'

describe('buildBackendLaunchSpec', () => {
  it('uses the packaged runtime and keeps secrets out of command arguments', () => {
    const spec = buildBackendLaunchSpec({
      isPackaged: true,
      resourcesPath: '/Applications/ResumeGo.app/Contents/Resources',
      projectRoot: '/workspace',
      dataDir: '/Users/test/Library/Application Support/ResumeGo',
      port: 43123,
      workspaceToken: 'session-secret',
      internalToken: 'internal-secret',
      platform: 'darwin',
    })

    expect(spec.command).toBe('/Applications/ResumeGo.app/Contents/Resources/runtime/bin/java')
    expect(spec.args).toEqual([
      '-jar',
      '/Applications/ResumeGo.app/Contents/Resources/backend/resume-go.jar',
      '--spring.profiles.active=local',
      '--server.port=43123',
    ])
    expect(spec.env.RESUMEGO_DB_PATH).toBe(path.join(spec.env.RESUMEGO_DATA_DIR, 'resumego'))
    expect(spec.env.LOCAL_WORKSPACE_TOKEN).toBe('session-secret')
    expect(spec.env.RESUMEGO_INTERNAL_TOKEN).toBe('internal-secret')
    expect(spec.args.join(' ')).not.toContain('session-secret')
  })

  it('uses the system java executable for development', () => {
    const spec = buildBackendLaunchSpec({
      isPackaged: false,
      resourcesPath: '/unused',
      projectRoot: '/workspace',
      dataDir: '/tmp/resumego-dev',
      port: 12345,
      workspaceToken: 'dev-token',
      internalToken: 'dev-internal-token',
      platform: 'win32',
    })

    expect(spec.command).toBe('java')
    expect(spec.args[1]).toBe('/workspace/backend/target/resume-go.jar')
  })
})
