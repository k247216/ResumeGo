import { contextBridge, ipcRenderer } from 'electron'

contextBridge.exposeInMainWorld('resumeGoDesktop', {
  runtime: () => ipcRenderer.sendSync('resumego:runtime-config'),
  saveApiKey: (profileId: number, apiKey: string) => ipcRenderer.invoke('resumego:key-save', profileId, apiKey),
  deleteApiKey: (profileId: number) => ipcRenderer.invoke('resumego:key-delete', profileId),
  hasApiKey: (profileId: number) => ipcRenderer.invoke('resumego:key-has', profileId),
  applyApiKey: (profileId: number) => ipcRenderer.invoke('resumego:key-apply', profileId),
  keyStorageMode: () => ipcRenderer.invoke('resumego:key-storage-mode'),
  listBackups: () => ipcRenderer.invoke('resumego:backup-list'),
  createBackup: () => ipcRenderer.invoke('resumego:backup-create'),
  restoreBackup: (backupId: string) => ipcRenderer.invoke('resumego:backup-restore', backupId),
  exportBackup: (backupId: string | null) => ipcRenderer.invoke('resumego:backup-export', backupId),
  openKnowledgeSource: (documentId: number) => ipcRenderer.invoke('resumego:knowledge-open-source', documentId),
  revealKnowledgeSource: (documentId: number) => ipcRenderer.invoke('resumego:knowledge-reveal-source', documentId),
})
