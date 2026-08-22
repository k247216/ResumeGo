<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  applyWebSessionKey,
  createAiProvider,
  deleteAiProvider,
  fetchAiProviderModels,
  listAiProviders,
  setDefaultAiProvider,
  testAiProvider,
  updateAiProvider,
  type AiProtocol,
  type AiProviderProfile,
} from '../../api/aiProviders'
import { aiProviderPresets } from '../../utils/aiProviderPresets'
import PageHeader from '../../components/PageHeader.vue'

const profiles = ref<AiProviderProfile[]>([])
const loading = ref(true)
const saving = ref(false)
const testing = ref(false)
const fetchingModels = ref(false)
const availableModels = ref<string[]>([])
const keyStorageMode = ref<'web' | 'secure' | 'session'>('web')
const selectedId = ref<number | null>(null)
const presetId = ref('')
const addingNew = ref(false)
const connected = ref(false)
const showAdvanced = ref(false)
const activeSection = ref<'general' | 'ai'>('ai')
const isDesktop = computed(() => Boolean(window.resumeGoDesktop))

interface BackupInfo {
  id: string
  createdAt: string
  sizeBytes: number
}

const backups = ref<BackupInfo[]>([])
const backupsLoading = ref(false)
const backupBusy = ref(false)

async function loadBackups() {
  if (!window.resumeGoDesktop) return
  backupsLoading.value = true
  try {
    backups.value = await window.resumeGoDesktop.listBackups()
  } catch {
    backups.value = []
  } finally {
    backupsLoading.value = false
  }
}

async function createBackupNow() {
  if (!window.resumeGoDesktop || backupBusy.value) return
  backupBusy.value = true
  try {
    await window.resumeGoDesktop.createBackup()
    await loadBackups()
    ElMessage.success('已创建本地备份')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '创建备份失败')
  } finally {
    backupBusy.value = false
  }
}

async function exportWorkspace() {
  if (!window.resumeGoDesktop || backupBusy.value) return
  backupBusy.value = true
  try {
    const result = await window.resumeGoDesktop.exportBackup(null)
    if (result && !result.canceled) {
      ElMessage.success('工作区已导出到 ' + result.exportedTo)
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '导出失败')
  } finally {
    backupBusy.value = false
  }
}

async function restoreBackup(backupId: string) {
  if (!window.resumeGoDesktop || backupBusy.value) return
  try {
    await ElMessageBox.confirm(
      '恢复会先保留当前数据到崩溃现场目录，再用所选备份覆盖工作区。恢复后需要重新启动应用。确定继续吗？',
      '恢复备份',
      { confirmButtonText: '恢复并重启', cancelButtonText: '取消', type: 'warning' },
    )
  } catch {
    return
  }
  backupBusy.value = true
  try {
    await window.resumeGoDesktop.restoreBackup(backupId)
    ElMessage.success('备份已恢复，正在重启应用...')
    setTimeout(() => window.location.reload(), 800)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '恢复失败')
  } finally {
    backupBusy.value = false
  }
}
const hasProfiles = computed(() => profiles.value.length > 0)
const selected = computed(() => profiles.value.find((profile) => profile.id === selectedId.value) ?? null)
const active = computed(() => profiles.value.find((profile) => profile.defaultProfile) ?? null)
const form = reactive({
  displayName: '',
  protocolType: 'openai-compatible' as AiProtocol,
  baseUrl: '',
  defaultModel: '',
  apiKey: '',
})

function choosePreset(id: string) {
  const preset = aiProviderPresets.find((item) => item.id === id)
  if (!preset) return
  presetId.value = id
  if (id !== 'custom') form.displayName = preset.label
  form.protocolType = preset.protocolType
  form.baseUrl = preset.baseUrl
  // 模型名称由服务商提供或用户填写，不预设任何模型 ID
  form.defaultModel = ''
  availableModels.value = []
  connected.value = false
  // 自定义兼容服务需要用户填写 Base URL，直接进入高级设置
  showAdvanced.value = id === 'custom'
}

function startAdd() {
  selectedId.value = null
  addingNew.value = true
  connected.value = false
  showAdvanced.value = false
  presetId.value = ''
  form.displayName = ''
  form.protocolType = 'openai-compatible'
  form.baseUrl = ''
  form.defaultModel = ''
  form.apiKey = ''
  availableModels.value = []
}

function cancelAdd() {
  addingNew.value = false
  connected.value = false
  showAdvanced.value = false
  selectedId.value = null
  if (profiles.value.length) editProfile(profiles.value[0]!)
}

function editProfile(profile: AiProviderProfile) {
  selectedId.value = profile.id
  addingNew.value = false
  connected.value = true
  showAdvanced.value = false
  form.displayName = profile.displayName
  form.protocolType = profile.protocolType
  form.baseUrl = profile.baseUrl
  form.defaultModel = profile.defaultModel
  form.apiKey = ''
  availableModels.value = []
}

async function loadProfiles() {
  loading.value = true
  try {
    const loaded = await listAiProviders()
    profiles.value = window.resumeGoDesktop
      ? await Promise.all(loaded.map(async (profile) => ({
          ...profile,
          apiKeyConfigured: await window.resumeGoDesktop!.hasApiKey(profile.id),
        })))
      : loaded
    if (profiles.value.length && selectedId.value === null && !addingNew.value) {
      editProfile(profiles.value[0]!)
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '读取模型配置失败')
  } finally {
    loading.value = false
  }
}

function probeInput() {
  return {
    displayName: form.displayName,
    protocolType: form.protocolType,
    baseUrl: form.baseUrl,
    defaultModel: form.defaultModel,
    apiKey: form.apiKey,
  }
}

async function save() {
  if (!form.displayName.trim() || !form.baseUrl.trim() || !form.defaultModel.trim()) {
    ElMessage.warning('请完整填写配置名称、Base URL 和模型名称')
    return
  }
  saving.value = true
  try {
    const input = {
      displayName: form.displayName,
      protocolType: form.protocolType,
      baseUrl: form.baseUrl,
      defaultModel: form.defaultModel,
    }
    let profile = selectedId.value
      ? await updateAiProvider(selectedId.value, input)
      : await createAiProvider(input)
    profile = await setDefaultAiProvider(profile.id)
    if (form.apiKey.trim()) {
      if (window.resumeGoDesktop) await window.resumeGoDesktop.saveApiKey(profile.id, form.apiKey)
      else await applyWebSessionKey(profile.id, form.apiKey)
    } else if (window.resumeGoDesktop && selectedId.value) {
      await window.resumeGoDesktop.applyApiKey(profile.id)
    }
    selectedId.value = profile.id
    addingNew.value = false
    connected.value = true
    form.apiKey = ''
    await loadProfiles()
    ElMessage.success('模型配置已保存并设为默认')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function verifyAndContinue() {
  if (!form.baseUrl.trim()) {
    ElMessage.warning('请先选择服务商')
    return
  }
  if (!form.displayName.trim()) {
    ElMessage.warning('请填写配置名称')
    return
  }
  if (!form.apiKey.trim()) {
    ElMessage.warning('请输入 API Key 以验证服务')
    return
  }
  fetchingModels.value = true
  try {
    const result = await fetchAiProviderModels(probeInput())
    availableModels.value = result.models
    connected.value = true
    if (result.models.length) {
      ElMessage.success('连接成功，请选择要使用的模型')
    } else {
      ElMessage.info('连接成功；未获取到模型列表，可在高级设置中手动填写模型名称')
      showAdvanced.value = true
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '连接失败')
    showAdvanced.value = true
  } finally {
    fetchingModels.value = false
  }
}

async function testConnection() {
  if (selectedId.value === null) return
  testing.value = true
  try {
    if (window.resumeGoDesktop && !form.apiKey.trim()) {
      await window.resumeGoDesktop.applyApiKey(selectedId.value)
    }
    const result = await testAiProvider(selectedId.value)
    await loadProfiles()
    if (result.lastTestStatus === 'success') ElMessage.success('连接成功')
    else ElMessage.error(result.lastTestMessage || '连接失败')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '连接测试失败')
  } finally {
    testing.value = false
  }
}

async function fetchModels() {
  if (!form.apiKey.trim()) {
    ElMessage.warning('获取模型需要重新输入 API Key；密钥不会从安全存储回填')
    return
  }
  fetchingModels.value = true
  try {
    const result = await fetchAiProviderModels(probeInput())
    availableModels.value = result.models
    ElMessage.success(result.message)
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '获取模型失败；你仍可手工填写模型名称')
  } finally {
    fetchingModels.value = false
  }
}

async function remove(profile: AiProviderProfile) {
  await ElMessageBox.confirm(`删除“${profile.displayName}”？简历和求职目标不会受到影响。`, '删除模型配置', {
    confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning',
  })
  try {
    if (window.resumeGoDesktop) await window.resumeGoDesktop.deleteApiKey(profile.id)
    await deleteAiProvider(profile.id)
    if (selectedId.value === profile.id) {
      selectedId.value = null
      addingNew.value = false
      connected.value = false
    }
    await loadProfiles()
    ElMessage.success('模型配置已删除')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败，请重试')
  }
}

function formatBackupTime(iso: string): string {
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return iso
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function formatBackupSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1024 / 1024).toFixed(1) + ' MB'
}

onMounted(async () => {
  if (window.resumeGoDesktop) keyStorageMode.value = await window.resumeGoDesktop.keyStorageMode()
  await loadProfiles()
  await loadBackups()
})
</script>

<template>
  <section class="settings-view">
    <PageHeader eyebrow="本地设置" title="设备与模型服务" subtitle="简历与求职目标保存在本机；AI 仅在你主动使用功能时调用所选服务。">
      <template #actions>
        <div class="runtime-badge"><span></span>{{ isDesktop ? '桌面安全模式' : 'Web 开发模式' }}</div>
      </template>
    </PageHeader>

    <div class="settings-body">
      <nav class="settings-nav">
        <button type="button" :class="{ selected: activeSection === 'general' }" @click="activeSection = 'general'">常规</button>
        <button type="button" :class="{ selected: activeSection === 'ai' }" @click="activeSection = 'ai'">AI 模型服务</button>
      </nav>

      <main class="settings-content">
        <!-- 常规 -->
        <section v-show="activeSection === 'general'" class="general-section">
          <h3 class="section-title">本地数据</h3>
          <div class="flat-row">
            <strong>存储位置</strong>
            <span>H2 文件工作区 · 启动前自动保留最近 5 份冷备份</span>
            <em>已启用</em>
          </div>
          <div class="flat-row">
            <strong>运行模式</strong>
            <span>{{ isDesktop ? '桌面端安全存储可用，密钥不进数据库' : '开发模式，密钥仅存于本地服务内存' }}</span>
            <em>{{ isDesktop ? '桌面安全模式' : 'Web 开发模式' }}</em>
          </div>

          <div v-if="isDesktop" class="backup-panel">
            <div class="backup-head">
              <div>
                <h3 class="section-title">数据备份</h3>
                <p class="backup-desc">备份保存在本机工作区目录，可随时手动创建、导出或恢复到历史版本。</p>
              </div>
              <div class="backup-actions">
                <button type="button" class="backup-btn" data-test="backup-create" :disabled="backupBusy" @click="createBackupNow">
                  <span v-if="backupBusy">处理中...</span>
                  <span v-else>＋ 创建备份</span>
                </button>
                <button type="button" class="backup-btn" data-test="backup-export" :disabled="backupBusy" @click="exportWorkspace">导出工作区</button>
              </div>
            </div>

            <div v-if="backupsLoading" class="backup-empty">正在读取备份...</div>
            <div v-else-if="backups.length === 0" class="backup-empty">暂无手动备份。应用启动时会自动保留最近 5 份冷备份。</div>
            <ul v-else class="backup-list">
              <li v-for="backup in backups" :key="backup.id" class="backup-item">
                <span class="backup-time">{{ formatBackupTime(backup.createdAt) }}</span>
                <span class="backup-size">{{ formatBackupSize(backup.sizeBytes) }}</span>
                <button type="button" class="backup-restore" :disabled="backupBusy" @click="restoreBackup(backup.id)">恢复</button>
              </li>
            </ul>
          </div>

          <div v-else class="backup-web-note">
            <p>数据备份与恢复仅桌面版可用。开发模式下数据保存在本地服务数据库，请直接备份数据目录。</p>
          </div>
        </section>

        <!-- AI 模型服务 -->
        <section v-show="activeSection === 'ai'" class="ai-section">
          <div class="ai-head">
            <div>
              <p class="eyebrow">AI 模型</p>
              <h2>使用你自己的 API</h2>
            </div>
            <span :class="['provider-state', active?.apiKeyConfigured ? 'ready' : '']">
              <template v-if="active?.apiKeyConfigured"><i class="connected-dot" aria-hidden="true"></i>{{ active.displayName }} 可用</template>
              <template v-else>尚未配置</template>
            </span>
          </div>

          <!-- 尚未配置任何服务：两栏（导航 | 设置工作区），不出现半填满的配置表单 -->
          <div v-if="!hasProfiles" class="setup-workspace">
            <div v-if="!addingNew" class="setup-empty" data-test="setup-empty">
              <div class="setup-copy">
                <strong>尚未配置任何服务</strong>
                <p>AI 功能只会在你主动使用时调用已配置的服务。你只需要一个兼容 OpenAI、Anthropic 或 Gemini 的 API 服务商：填入服务商与 API Key，验证后从服务商获取模型即可开始。</p>
              </div>
              <button type="button" class="setup-add" data-test="add-ai-service" @click="startAdd">＋ 添加 AI 服务</button>
            </div>

            <div v-else class="provider-form connect-form" data-test="connect-form">
              <!-- 第 1 步：服务商 + API Key + 验证 -->
              <div v-if="!connected" class="connect-step">
                <p class="step-label">第 1 步 · 选择服务商并验证 API Key</p>
                <label>服务商
                  <select :value="presetId" @change="choosePreset(($event.target as HTMLSelectElement).value)">
                    <option value="" disabled>选择服务商</option>
                    <option v-for="preset in aiProviderPresets" :key="preset.id" :value="preset.id">{{ preset.label }}</option>
                  </select>
                </label>
                <label>配置名称<input v-model="form.displayName" maxlength="80" placeholder="例如：我的 DeepSeek" /></label>
                <label>API Key
                  <input v-model="form.apiKey" type="password" autocomplete="off" placeholder="粘贴 API Key" />
                  <small>密钥只发送给你选择的 API 服务商，用于验证与调用；不会进入普通日志或备份。</small>
                </label>
                <div v-if="showAdvanced" class="advanced-block">
                  <label>协议
                    <select v-model="form.protocolType">
                      <option value="openai-compatible">OpenAI 兼容</option>
                      <option value="anthropic">Anthropic</option>
                      <option value="gemini">Google Gemini</option>
                    </select>
                  </label>
                  <label>Base URL<input v-model="form.baseUrl" maxlength="500" placeholder="https://api.example.com/v1" /></label>
                  <label>模型名称
                    <div class="model-input-row">
                      <el-select
  v-model="form.defaultModel"
  filterable
  allow-create
  default-first-option
  placeholder="选择或输入模型 ID"
  data-test="provider-model-select"
  class="provider-model-select"
>
  <el-option v-for="model in availableModels" :key="model" :label="model" :value="model" />
</el-select>
                    </div>
                    <small>连接成功但未获取到模型列表时，可在此手工填写模型 ID。</small>
                  </label>
                </div>
                <div class="form-actions">
                  <button type="button" class="cancel-link" @click="cancelAdd">取消</button>
                  <span></span>
                  <button
                    v-if="!showAdvanced" type="button" class="primary" data-test="verify-and-continue"
                    :disabled="fetchingModels" @click="verifyAndContinue"
                  >{{ fetchingModels ? '验证中…' : '验证并继续' }}</button>
                  <template v-else>
                    <button type="button" :disabled="fetchingModels" @click="verifyAndContinue">{{ fetchingModels ? '验证中…' : '重新验证' }}</button>
                    <button type="button" class="primary" data-test="save-config" :disabled="saving" @click="save">{{ saving ? '保存中…' : '保存配置' }}</button>
                  </template>
                </div>
              </div>

              <!-- 第 2 步：已连接，选择模型（模型来自服务商，不预设） -->
              <div v-else class="connect-step">
                <p class="step-label">第 2 步 · 选择要使用的模型</p>
                <div class="connected-banner" data-test="connected-banner">
                  <i class="connected-dot" aria-hidden="true"></i>{{ form.displayName }} 已连接
                </div>
                <label>模型名称
                  <div class="model-input-row">
                    <el-select
  v-model="form.defaultModel"
  filterable
  allow-create
  default-first-option
  placeholder="选择或输入模型 ID"
  data-test="provider-model-select"
  class="provider-model-select"
>
  <el-option v-for="model in availableModels" :key="model" :label="model" :value="model" />
</el-select>
                    <button type="button" :disabled="fetchingModels" @click="fetchModels">{{ fetchingModels ? '获取中…' : '刷新模型' }}</button>
                  </div>
                  
                  <small>模型列表由服务商返回；也可以直接输入模型 ID。</small>
                </label>
                <div class="advanced-block">
                  <button type="button" class="advanced-toggle" data-test="advanced-toggle" @click="showAdvanced = !showAdvanced">高级设置 {{ showAdvanced ? '收起' : '展开' }}</button>
                  <div v-show="showAdvanced" class="advanced-fields">
                    <label>协议
                      <select v-model="form.protocolType">
                        <option value="openai-compatible">OpenAI 兼容</option>
                        <option value="anthropic">Anthropic</option>
                        <option value="gemini">Google Gemini</option>
                      </select>
                    </label>
                    <label>Base URL<input v-model="form.baseUrl" maxlength="500" placeholder="https://api.example.com/v1" /></label>
                  </div>
                </div>
                <div class="form-actions">
                  <button type="button" class="cancel-link" @click="cancelAdd">取消</button>
                  <span></span>
                  <button type="button" class="primary" data-test="save-config" :disabled="saving" @click="save">{{ saving ? '保存中…' : '保存配置' }}</button>
                </div>
              </div>
            </div>
          </div>

          <!-- 已配置服务：三栏（导航 | 服务列表 | 配置表单） -->
          <div v-else class="ai-grid">
            <aside class="profile-list" v-loading="loading">
              <button class="new-profile" type="button" data-test="add-ai-service" @click="startAdd">＋ 添加服务</button>
              <button
                v-for="profile in profiles" :key="profile.id" type="button"
                :class="['profile-item', { selected: selectedId === profile.id }]" @click="editProfile(profile)"
              >
                <span class="profile-copy"><strong>{{ profile.displayName }}</strong><small>{{ profile.defaultModel }}</small></span>
                <span class="profile-badges">
                  <i v-if="profile.apiKeyConfigured" class="connected-dot" title="已连接" aria-hidden="true"></i>
                  <i v-if="profile.defaultProfile" class="default-tag">默认</i>
                </span>
              </button>
            </aside>

            <div class="provider-form">
              <!-- 正在添加新服务 -->
              <template v-if="addingNew">
                <!-- 第 1 步：服务商 + API Key + 验证 -->
                <div v-if="!connected" class="connect-step">
                  <p class="step-label">第 1 步 · 选择服务商并验证 API Key</p>
                  <label>服务商
                    <select :value="presetId" @change="choosePreset(($event.target as HTMLSelectElement).value)">
                      <option value="" disabled>选择服务商</option>
                      <option v-for="preset in aiProviderPresets" :key="preset.id" :value="preset.id">{{ preset.label }}</option>
                    </select>
                  </label>
                  <label>配置名称<input v-model="form.displayName" maxlength="80" placeholder="例如：我的 DeepSeek" /></label>
                  <label>API Key
                    <input v-model="form.apiKey" type="password" autocomplete="off" placeholder="粘贴 API Key" />
                    <small>密钥只发送给你选择的 API 服务商，用于验证与调用；不会进入普通日志或备份。</small>
                  </label>
                  <div v-if="showAdvanced" class="advanced-block">
                    <label>协议
                      <select v-model="form.protocolType">
                        <option value="openai-compatible">OpenAI 兼容</option>
                        <option value="anthropic">Anthropic</option>
                        <option value="gemini">Google Gemini</option>
                      </select>
                    </label>
                    <label>Base URL<input v-model="form.baseUrl" maxlength="500" placeholder="https://api.example.com/v1" /></label>
                    <label>模型名称
                      <div class="model-input-row">
                        <el-select
  v-model="form.defaultModel"
  filterable
  allow-create
  default-first-option
  placeholder="选择或输入模型 ID"
  data-test="provider-model-select"
  class="provider-model-select"
>
  <el-option v-for="model in availableModels" :key="model" :label="model" :value="model" />
</el-select>
                      </div>
                      <small>连接成功但未获取到模型列表时，可在此手工填写模型 ID。</small>
                    </label>
                  </div>
                  <div class="form-actions">
                    <button type="button" class="cancel-link" @click="cancelAdd">取消</button>
                    <span></span>
                    <button
                      v-if="!showAdvanced" type="button" class="primary" data-test="verify-and-continue"
                      :disabled="fetchingModels" @click="verifyAndContinue"
                    >{{ fetchingModels ? '验证中…' : '验证并继续' }}</button>
                    <template v-else>
                      <button type="button" :disabled="fetchingModels" @click="verifyAndContinue">{{ fetchingModels ? '验证中…' : '重新验证' }}</button>
                      <button type="button" class="primary" data-test="save-config" :disabled="saving" @click="save">{{ saving ? '保存中…' : '保存配置' }}</button>
                    </template>
                  </div>
                </div>

                <!-- 第 2 步：已连接，选择模型 -->
                <div v-else class="connect-step">
                  <p class="step-label">第 2 步 · 选择要使用的模型</p>
                  <div class="connected-banner" data-test="connected-banner">
                    <i class="connected-dot" aria-hidden="true"></i>{{ form.displayName }} 已连接
                  </div>
                  <label>模型名称
                    <div class="model-input-row">
                      <el-select
  v-model="form.defaultModel"
  filterable
  allow-create
  default-first-option
  placeholder="选择或输入模型 ID"
  data-test="provider-model-select"
  class="provider-model-select"
>
  <el-option v-for="model in availableModels" :key="model" :label="model" :value="model" />
</el-select>
                      <button type="button" :disabled="fetchingModels" @click="fetchModels">{{ fetchingModels ? '获取中…' : '刷新模型' }}</button>
                    </div>
                    
                    <small>模型列表由服务商返回；也可以直接输入模型 ID。</small>
                  </label>
                  <div class="advanced-block">
                    <button type="button" class="advanced-toggle" data-test="advanced-toggle" @click="showAdvanced = !showAdvanced">高级设置 {{ showAdvanced ? '收起' : '展开' }}</button>
                    <div v-show="showAdvanced" class="advanced-fields">
                      <label>协议
                        <select v-model="form.protocolType">
                          <option value="openai-compatible">OpenAI 兼容</option>
                          <option value="anthropic">Anthropic</option>
                          <option value="gemini">Google Gemini</option>
                        </select>
                      </label>
                      <label>Base URL<input v-model="form.baseUrl" maxlength="500" placeholder="https://api.example.com/v1" /></label>
                    </div>
                  </div>
                  <div class="form-actions">
                    <button type="button" class="cancel-link" @click="cancelAdd">取消</button>
                    <span></span>
                    <button type="button" class="primary" data-test="save-config" :disabled="saving" @click="save">{{ saving ? '保存中…' : '保存配置' }}</button>
                  </div>
                </div>
              </template>

              <!-- 编辑已保存的配置 -->
              <template v-else-if="selected">
                <div v-if="connected" class="connected-banner" data-test="connected-banner">
                  <i class="connected-dot" aria-hidden="true"></i>{{ selected.displayName }} 已连接
                </div>
                <label>配置名称<input v-model="form.displayName" maxlength="80" placeholder="例如：我的 DeepSeek" /></label>
                <div class="form-row">
                  <label>协议
                    <select v-model="form.protocolType">
                      <option value="openai-compatible">OpenAI 兼容</option>
                      <option value="anthropic">Anthropic</option>
                      <option value="gemini">Google Gemini</option>
                    </select>
                  </label>
                  <label>Base URL<input v-model="form.baseUrl" maxlength="500" placeholder="https://api.example.com/v1" /></label>
                </div>
                <label>模型名称
                  <div class="model-input-row">
                    <el-select
  v-model="form.defaultModel"
  filterable
  allow-create
  default-first-option
  placeholder="选择或输入模型 ID"
  data-test="provider-model-select"
  class="provider-model-select"
>
  <el-option v-for="model in availableModels" :key="model" :label="model" :value="model" />
</el-select>
                    <button type="button" :disabled="fetchingModels" @click="fetchModels">{{ fetchingModels ? '获取中…' : '获取模型' }}</button>
                  </div>
                  
                </label>
                <label>API Key
                  <input v-model="form.apiKey" type="password" autocomplete="off" :placeholder="selected?.apiKeyConfigured ? '已安全保存；留空表示不修改' : '粘贴 API Key'" />
                  <small v-if="keyStorageMode === 'secure'">密钥由系统安全存储加密，不进入数据库和普通备份。</small>
                  <small v-else-if="keyStorageMode === 'session'">系统安全存储不可用：密钥仅在本次应用运行期间保留，退出后需重新输入。</small>
                  <small v-else>开发模式：密钥只保存在本地服务内存，服务重启后需重新输入。</small>
                </label>
                <div v-if="selected?.lastTestMessage" :class="['test-result', selected.lastTestStatus]">
                  {{ selected.lastTestMessage }}
                </div>
                <div class="form-actions">
                  <button class="danger" type="button" @click="remove(selected)">删除</button>
                  <span></span>
                  <button type="button" :disabled="testing" @click="testConnection">{{ testing ? '测试中…' : '测试连接' }}</button>
                  <button class="primary" type="button" :disabled="saving" @click="save">{{ saving ? '保存中…' : '保存并设为默认' }}</button>
                </div>
              </template>

              <!-- 有服务但尚未选中：引导选择 -->
              <div v-else class="unselected-hint">
                <p>从左侧选择一个服务配置进行编辑，或点击「＋ 添加服务」配置新的 AI 服务。</p>
              </div>
            </div>
          </div>
        </section>
      </main>
    </div>
  </section>
</template>

<style scoped>
.settings-view{display:flex;flex-direction:column;height:100%;min-height:0}
.settings-body{flex:1;min-height:0;display:grid;grid-template-columns:200px minmax(0,1fr);border-top:1px solid var(--border-subtle)}

/* ── 左列：节导航 ── */
.settings-nav{min-height:0;overflow-y:auto;border-right:1px solid var(--border-subtle);padding:14px 12px 24px}
.settings-nav button{display:block;width:100%;text-align:left;border:0;border-radius:var(--radius-control);background:transparent;color:var(--copy);padding:10px 12px;font-size:13px;font-weight:500;cursor:pointer}
.settings-nav button:hover{background:var(--bg-hover)}
.settings-nav button.selected{background:var(--bg-selected);color:var(--brand);font-weight:600}

/* ── 右列：内容 ── */
.settings-content{min-height:0;overflow-y:auto;padding:22px 30px 48px}
.section-title{margin:0 0 4px;font-size:12px;font-weight:600;letter-spacing:.06em;color:var(--muted)}
.general-section{padding:0 4px;max-width:760px}
.flat-row{display:flex;align-items:baseline;gap:14px;padding:15px 2px}
.flat-row + .flat-row{border-top:1px solid var(--border-subtle)}
.flat-row strong{flex:0 0 96px;font-size:14px;font-weight:600;color:var(--ink)}
.flat-row span{flex:1;min-width:0;color:var(--copy);font-size:13px;line-height:1.55}
.flat-row em{flex:0 0 auto;color:var(--brand);font-size:12px;font-weight:600;font-style:normal}

.backup-panel{margin-top:22px;border-top:1px solid var(--border-subtle);padding-top:18px}
.backup-head{display:flex;align-items:flex-start;justify-content:space-between;gap:16px;margin-bottom:12px}
.backup-desc{margin:6px 0 0;color:var(--muted);font-size:12px;line-height:1.6}
.backup-actions{display:flex;gap:8px;flex:0 0 auto}
.backup-btn{display:inline-flex;align-items:center;gap:6px;padding:7px 12px;border:1px solid var(--border-default);border-radius:var(--radius-control);background:var(--bg-surface);color:var(--ink);font-size:12px;font-weight:600;cursor:pointer}
.backup-btn:hover:not(:disabled){border-color:var(--brand);color:var(--brand)}
.backup-btn:disabled{opacity:.5;cursor:not-allowed}
.backup-empty{padding:18px 4px;color:var(--muted);font-size:13px;line-height:1.6}
.backup-list{margin:0;padding:0;list-style:none}
.backup-item{display:flex;align-items:center;gap:12px;padding:11px 2px;border-top:1px solid var(--border-subtle)}
.backup-item:first-child{border-top:0}
.backup-time{flex:1;font-size:13px;font-weight:600;color:var(--ink)}
.backup-size{color:var(--muted);font-size:12px}
.backup-restore{border:0;background:transparent;color:var(--brand);font-size:12px;font-weight:600;cursor:pointer;padding:4px 8px;border-radius:6px}
.backup-restore:hover:not(:disabled){background:var(--brand-soft)}
.backup-restore:disabled{opacity:.5;cursor:not-allowed}
.backup-web-note{margin-top:16px;padding:14px;border:1px dashed var(--border-default);border-radius:var(--radius-panel);color:var(--muted);font-size:12px;line-height:1.6}

.ai-head{display:flex;align-items:flex-end;justify-content:space-between;gap:24px;padding-bottom:16px}
.eyebrow{margin:0;color:var(--brand);font-size:12px;font-weight:600;letter-spacing:.08em}
.ai-head h2{margin:5px 0 0;font-size:22px;font-weight:650;letter-spacing:-.02em;color:var(--ink)}
.provider-state{flex:0 0 auto;display:inline-flex;align-items:center;gap:7px;color:var(--muted);font-size:12px;font-weight:600}
.provider-state.ready{color:var(--brand)}

.connected-dot{flex:0 0 auto;display:inline-block;width:7px;height:7px;border-radius:50%;background:var(--brand)}

/* ── 尚未配置任何服务：两栏设置工作区 ── */
.setup-workspace{max-width:640px;padding-top:6px}
.setup-empty{display:grid;gap:16px;padding:30px 28px;border:1px dashed var(--border-default);border-radius:var(--radius-panel)}
.setup-copy{display:grid;gap:6px}
.setup-copy strong{color:var(--ink);font-size:15px;font-weight:650}
.setup-copy p{margin:0;color:var(--muted);font-size:13px;line-height:1.65}
.setup-add{justify-self:start;padding:10px 18px;border:0;border-radius:var(--radius-control);background:var(--brand);color:#fff;font-size:13px;font-weight:600;cursor:pointer}
.setup-add:hover{background:var(--accent-hover)}

/* ── 连接引导表单 ── */
.connect-form{max-width:560px;padding:6px 0 0}
.connect-step{display:grid;gap:15px}
.step-label{margin:0;color:var(--muted);font-size:12px;font-weight:600;letter-spacing:.04em}
.connected-banner{display:inline-flex;align-items:center;gap:8px;justify-self:start;padding:9px 13px;border-radius:var(--radius-control);background:var(--accent-soft);color:var(--brand);font-size:13px;font-weight:600}
.advanced-block{display:grid;gap:15px;margin-top:2px;padding-top:15px;border-top:1px solid var(--border-subtle)}
.advanced-toggle{justify-self:start;padding:4px 2px;border:0;background:transparent;color:var(--brand);font-size:12px;font-weight:600;cursor:pointer}
.advanced-fields{display:grid;gap:15px}
.cancel-link{padding:4px 2px;border:0;background:transparent;color:var(--muted);font-size:13px;cursor:pointer}
.cancel-link:hover{color:var(--ink)}
.unselected-hint{padding:24px 4px;color:var(--muted);font-size:13px;line-height:1.6}

/* ── 已配置服务：三栏 ── */
.ai-grid{display:grid;grid-template-columns:240px minmax(0,1fr);gap:0}
.profile-list{min-height:320px;padding:6px 16px 24px 0;border-right:1px solid var(--border-subtle)}
.profile-list button{width:100%;border:0;text-align:left}
.new-profile{margin-bottom:12px;padding:10px 12px;border-radius:var(--radius-control)!important;background:var(--accent-soft);color:var(--brand);font-weight:600}
.profile-item{display:flex;align-items:center;justify-content:space-between;gap:8px;margin:3px 0;padding:11px 12px;border-radius:var(--radius-control);background:transparent;color:var(--copy);cursor:pointer}
.profile-item:hover{background:var(--bg-hover)}
.profile-item.selected{background:var(--bg-selected);color:var(--ink)}
.profile-copy{display:grid;gap:3px;min-width:0}
.profile-copy strong{font-size:13px;font-weight:600;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.profile-copy small{max-width:150px;overflow:hidden;color:var(--muted);text-overflow:ellipsis;white-space:nowrap;font-size:11px}
.profile-badges{display:inline-flex;align-items:center;gap:7px;flex:0 0 auto}
.default-tag{color:var(--brand);font-size:11px;font-style:normal}

.provider-form{display:grid;gap:15px;padding:6px 0 0 26px}
.provider-form label{display:grid;gap:7px;color:var(--copy);font-size:13px;font-weight:600}
.provider-form input,.provider-form select{box-sizing:border-box;width:100%;padding:10px 12px;border:1px solid var(--border-default);border-radius:var(--radius-control);background:var(--bg-surface);color:var(--ink);font:inherit;font-weight:500}
.provider-form input:focus,.provider-form select:focus{outline:none;border-color:var(--brand)}
.provider-form label small{color:var(--muted);font-size:12px;font-weight:400}
.form-row{display:grid;grid-template-columns:1fr 1fr;gap:14px}
.model-input-row{display:grid;grid-template-columns:minmax(0,1fr) auto;gap:8px}
.provider-model-select{width:100%}
.model-input-row button{padding:0 13px;border:1px solid var(--border-default);border-radius:var(--radius-control);background:var(--bg-surface);color:var(--copy);font-weight:600;white-space:nowrap;cursor:pointer}
.model-input-row button:disabled{opacity:.5;cursor:default}
.form-actions{display:flex;align-items:center;gap:9px;margin-top:8px}
.form-actions>span{flex:1}
.form-actions button{padding:9px 15px;border:1px solid var(--border-default);border-radius:var(--radius-control);background:var(--bg-surface);color:var(--copy);font-weight:600;font-size:13px;cursor:pointer}
.form-actions button:hover{background:var(--bg-hover)}
.form-actions button:disabled{cursor:not-allowed;opacity:.5}
.form-actions .primary{border-color:var(--brand);background:var(--brand);color:#fff}
.form-actions .primary:hover{background:var(--accent-hover)}
.form-actions .danger{border-color:var(--danger-soft);color:var(--danger)}
.test-result{padding:10px 12px;border-radius:var(--radius-control);background:var(--bg-hover);color:var(--muted);font-size:13px}
.test-result.success{background:var(--accent-soft);color:var(--brand)}
.test-result.failed{background:var(--danger-soft);color:var(--danger)}

.runtime-badge{display:inline-flex;align-items:center;gap:7px;color:var(--muted);font-size:12px;font-weight:600}
.runtime-badge span{width:7px;height:7px;border-radius:50%;background:var(--brand)}

/* 共享断点阶梯：窄窗口下导航转为横向 */
@media (max-width: 959px) {
  .settings-body{grid-template-columns:minmax(0,1fr);grid-template-rows:auto minmax(0,1fr)}
  .settings-nav{display:flex;gap:4px;border-right:0;border-bottom:1px solid var(--border-subtle);padding:10px 14px;overflow:visible}
  .settings-nav button{width:auto;flex:0 0 auto}
  .ai-grid{grid-template-columns:minmax(0,1fr)}
  .profile-list{min-height:0;padding:0 0 14px;border-right:0;border-bottom:1px solid var(--border-subtle)}
  .provider-form{padding:16px 0 0}
  .ai-head{align-items:flex-start;flex-direction:column}
  .form-row{grid-template-columns:1fr}
}
</style>
