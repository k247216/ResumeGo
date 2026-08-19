import { contextBridge, ipcRenderer } from 'electron'

contextBridge.exposeInMainWorld('resumeGoDesktop', {
  runtime: () => ipcRenderer.sendSync('resumego:runtime-config'),
  saveApiKey: (profileId: number, apiKey: string) => ipcRenderer.invoke('resumego:key-save', profileId, apiKey),
  deleteApiKey: (profileId: number) => ipcRenderer.invoke('resumego:key-delete', profileId),
  hasApiKey: (profileId: number) => ipcRenderer.invoke('resumego:key-has', profileId),
  applyApiKey: (profileId: number) => ipcRenderer.invoke('resumego:key-apply', profileId),
  keyStorageMode: () => ipcRenderer.invoke('resumego:key-storage-mode'),
})
