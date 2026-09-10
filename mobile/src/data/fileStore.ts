// 简历以“用户上传的文件”为唯一真相：PDF/MD 原样存二进制，本地库只留元数据。
// 二进制放 IndexedDB（容量远大于 localStorage），换机备份仍走 JSON（元数据）+ 重新上传。
const DB_NAME = 'zhida-mobile-files'
const STORE = 'files'

function openDb(): Promise<IDBDatabase | null> {
  return new Promise((resolve) => {
    if (typeof indexedDB === 'undefined') { resolve(null); return }
    let req: IDBOpenDBRequest
    try { req = indexedDB.open(DB_NAME, 1) } catch { resolve(null); return }
    req.onupgradeneeded = () => {
      const db = req.result
      if (!db.objectStoreNames.contains(STORE)) db.createObjectStore(STORE)
    }
    req.onsuccess = () => resolve(req.result)
    req.onerror = () => resolve(null)
  })
}

export async function putFile(key: string, blob: Blob): Promise<void> {
  const db = await openDb(); if (!db) return
  await new Promise<void>((resolve, reject) => {
    const tx = db.transaction(STORE, 'readwrite')
    tx.objectStore(STORE).put(blob, key)
    tx.oncomplete = () => resolve()
    tx.onerror = () => reject(tx.error)
  })
  db.close()
}

export async function getFile(key: string): Promise<Blob | null> {
  const db = await openDb(); if (!db) return null
  const blob = await new Promise<Blob | null>((resolve, reject) => {
    const tx = db.transaction(STORE, 'readonly')
    const req = tx.objectStore(STORE).get(key)
    req.onsuccess = () => resolve((req.result as Blob) ?? null)
    req.onerror = () => reject(req.error)
  })
  db.close()
  return blob
}

export async function deleteFile(key: string): Promise<void> {
  const db = await openDb(); if (!db) return
  await new Promise<void>((resolve) => {
    const tx = db.transaction(STORE, 'readwrite')
    tx.objectStore(STORE).delete(key)
    tx.oncomplete = () => resolve()
    tx.onerror = () => resolve()
  })
  db.close()
}

export async function readAsText(key: string): Promise<string | null> {
  const blob = await getFile(key)
  if (!blob) return null
  return await blob.text()
}

/** 生成可预览/可分享的对象 URL；调用方负责在卸载时 revoke。 */
export async function objectUrl(key: string): Promise<string | null> {
  const blob = await getFile(key)
  if (!blob) return null
  return URL.createObjectURL(blob)
}
