import path from 'node:path'

export interface BackendLaunchOptions {
  isPackaged: boolean
  resourcesPath: string
  projectRoot: string
  dataDir: string
  port: number
  workspaceToken: string
  internalToken: string
  platform: NodeJS.Platform
}

export interface BackendLaunchSpec {
  command: string
  args: string[]
  env: NodeJS.ProcessEnv & {
    RESUMEGO_DATA_DIR: string
    RESUMEGO_DB_PATH: string
    LOCAL_WORKSPACE_TOKEN: string
    RESUMEGO_INTERNAL_TOKEN: string
  }
}

export function buildBackendLaunchSpec(options: BackendLaunchOptions): BackendLaunchSpec {
  const runtimeExecutable = options.platform === 'win32' ? 'java.exe' : 'java'
  const command = options.isPackaged
    ? path.join(options.resourcesPath, 'runtime', 'bin', runtimeExecutable)
    : 'java'
  const jarPath = options.isPackaged
    ? path.join(options.resourcesPath, 'backend', 'resume-go.jar')
    : path.join(options.projectRoot, 'backend', 'target', 'resume-go-0.0.1-SNAPSHOT.jar')

  return {
    command,
    args: [
      '-jar',
      jarPath,
      '--spring.profiles.active=local',
      `--server.port=${options.port}`,
    ],
    env: {
      ...process.env,
      RESUMEGO_DATA_DIR: options.dataDir,
      RESUMEGO_DB_PATH: path.join(options.dataDir, 'resumego'),
      LOCAL_WORKSPACE_TOKEN: options.workspaceToken,
      RESUMEGO_INTERNAL_TOKEN: options.internalToken,
    },
  }
}
