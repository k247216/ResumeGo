// @vitest-environment happy-dom

import { afterEach, describe, expect, it, vi } from 'vitest'
import { apiFetch, resolveApiUrl } from './http'

describe('desktop api transport', () => {
  afterEach(() => {
    delete window.resumeGoDesktop
    vi.unstubAllGlobals()
  })

  it('keeps relative URLs in web development', () => {
    expect(resolveApiUrl('/api/v1/resumes')).toBe('/api/v1/resumes')
  })

  it('targets the isolated local backend and injects its session token', async () => {
    window.resumeGoDesktop = {
      runtime: () => ({ backendOrigin: 'http://127.0.0.1:43123', workspaceToken: 'random-token' }),
      saveApiKey: vi.fn(),
      deleteApiKey: vi.fn(),
      hasApiKey: vi.fn(),
      applyApiKey: vi.fn(),
      keyStorageMode: vi.fn(),
      listBackups: vi.fn(),
      createBackup: vi.fn(),
      restoreBackup: vi.fn(),
      exportBackup: vi.fn(),
    }
    const fetchMock = vi.fn().mockResolvedValue(new Response())
    vi.stubGlobal('fetch', fetchMock)

    await apiFetch('/api/v1/resumes')

    expect(fetchMock).toHaveBeenCalledWith(
      'http://127.0.0.1:43123/api/v1/resumes',
      expect.objectContaining({
        headers: expect.objectContaining({}),
      }),
    )
    const headers = fetchMock.mock.calls[0]![1]!.headers as Headers
    expect(headers.get('X-Workspace-Token')).toBe('random-token')
  })
})
