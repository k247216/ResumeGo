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
  testUnsavedAiProvider,
  updateAiProvider,
  type AiProtocol,
  type AiProviderProfile,
} from '../../api/aiProviders'
import { aiProviderPresets } from '../../utils/aiProviderPresets'

const profiles = ref<AiProviderProfile[]>([])
const loading = ref(true)
const saving = ref(false)
const testing = ref(false)
const fetchingModels = ref(false)
const availableModels = ref<string[]>([])
const keyStorageMode = ref<'web' | 'secure' | 'session'>('web')
const selectedId = ref<number | null>(null)
const presetId = ref('deepseek')
const isDesktop = computed(() => Boolean(window.resumeGoDesktop))
const selected = computed(() => profiles.value.find((profile) => profile.id === selectedId.value) ?? null)
const active = computed(() => profiles.value.find((profile) => profile.defaultProfile) ?? null)
const form = reactive({
  displayName: 'DeepSeek',
  protocolType: 'openai-compatible' as AiProtocol,
  baseUrl: 'https://api.deepseek.com/v1',
  defaultModel: 'deepseek-chat',
  apiKey: '',
})

function choosePreset(id: string) {
  const preset = aiProviderPresets.find((item) => item.id === id)
  if (!preset) return
  presetId.value = id
  if (id !== 'custom') form.displayName = preset.label
  form.protocolType = preset.protocolType
  form.baseUrl = preset.baseUrl
  form.defaultModel = preset.model
  availableModels.value = []
}

function newProfile() {
  selectedId.value = null
  form.apiKey = ''
  choosePreset('deepseek')
}

function editProfile(profile: AiProviderProfile) {
  selectedId.value = profile.id
  presetId.value = 'custom'
  form.displayName = profile.displayName
  form.protocolType = profile.protocolType
  form.baseUrl = profile.baseUrl
  form.defaultModel = profile.defaultModel
  form.apiKey = ''
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
    form.apiKey = ''
    await loadProfiles()
    ElMessage.success('模型配置已保存并设为默认')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '保存失败')
  } finally {
    saving.value = false
  }
}

async function testConnection() {
  testing.value = true
  try {
    if (!selectedId.value && !form.apiKey.trim()) {
      ElMessage.warning('测试未保存的配置需要输入 API Key')
      return
    }
    if (selectedId.value && window.resumeGoDesktop && !form.apiKey.trim()) {
      await window.resumeGoDesktop.applyApiKey(selectedId.value)
    }
    const result = selectedId.value && !form.apiKey.trim()
      ? await testAiProvider(selectedId.value)
      : await testUnsavedAiProvider(probeInput())
    await loadProfiles()
    const success = 'lastTestStatus' in result ? result.lastTestStatus === 'success' : result.success
    const message = 'lastTestMessage' in result ? result.lastTestMessage : result.message
    if (success) ElMessage.success('连接成功')
    else ElMessage.error(message || '连接失败')
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
    if (result.models.length) form.defaultModel = result.models[0]!
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
    if (selectedId.value === profile.id) newProfile()
    await loadProfiles()
    ElMessage.success('模型配置已删除')
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '删除失败，请重试')
  }
}

onMounted(async () => {
  if (window.resumeGoDesktop) keyStorageMode.value = await window.resumeGoDesktop.keyStorageMode()
  await loadProfiles()
})
</script>

<template>
  <section class="settings-view">
    <header class="settings-header">
      <div>
        <p class="settings-eyebrow">本地设置</p>
        <h1>设备与模型服务</h1>
        <p>简历与求职目标保存在本机；AI 仅在你主动使用功能时调用所选服务。</p>
      </div>
      <div class="runtime-badge"><span></span>{{ isDesktop ? '桌面安全模式' : 'Web 开发模式' }}</div>
    </header>

    <div class="local-card">
      <div><strong>本地数据</strong><small>H2 文件工作区 · 启动前自动保留最近 5 份冷备份</small></div>
      <span>已启用</span>
    </div>

    <div class="model-section">
      <div class="section-heading">
        <div><p class="settings-eyebrow">AI 模型</p><h2>使用你自己的 API</h2></div>
        <span :class="['provider-state', active?.apiKeyConfigured ? 'ready' : '']">
          {{ active?.apiKeyConfigured ? `${active.displayName} 可用` : '尚未配置' }}
        </span>
      </div>

      <div class="model-grid">
        <aside class="profile-list" v-loading="loading">
          <button class="new-profile" type="button" @click="newProfile">＋ 新建模型配置</button>
          <button
            v-for="profile in profiles" :key="profile.id" type="button"
            :class="['profile-item', { selected: selectedId === profile.id }]" @click="editProfile(profile)"
          >
            <span><strong>{{ profile.displayName }}</strong><small>{{ profile.defaultModel }}</small></span>
            <i v-if="profile.defaultProfile">默认</i>
          </button>
          <p v-if="!loading && profiles.length === 0" class="empty-copy">还没有模型配置。本地编辑功能仍可正常使用。</p>
        </aside>

        <div class="provider-form">
          <label>服务预设
            <select :value="presetId" @change="choosePreset(($event.target as HTMLSelectElement).value)">
              <option v-for="preset in aiProviderPresets" :key="preset.id" :value="preset.id">{{ preset.label }}</option>
            </select>
          </label>
          <div class="form-row">
            <label>配置名称<input v-model="form.displayName" maxlength="80" placeholder="例如：我的 DeepSeek" /></label>
            <label>协议
              <select v-model="form.protocolType">
                <option value="openai-compatible">OpenAI 兼容</option>
                <option value="anthropic">Anthropic</option>
                <option value="gemini">Google Gemini</option>
              </select>
            </label>
          </div>
          <label>Base URL<input v-model="form.baseUrl" maxlength="500" placeholder="https://api.example.com/v1" /></label>
          <label>模型名称
            <div class="model-input-row">
              <input v-model="form.defaultModel" list="provider-models" maxlength="120" placeholder="模型 ID" />
              <button type="button" :disabled="fetchingModels" @click="fetchModels">{{ fetchingModels ? '获取中…' : '获取模型' }}</button>
            </div>
            <datalist id="provider-models"><option v-for="model in availableModels" :key="model" :value="model" /></datalist>
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
            <button v-if="selected" class="danger" type="button" @click="remove(selected)">删除</button>
            <span></span>
            <button type="button" :disabled="testing" @click="testConnection">{{ testing ? '测试中…' : '测试连接' }}</button>
            <button class="primary" type="button" :disabled="saving" @click="save">{{ saving ? '保存中…' : '保存并设为默认' }}</button>
          </div>
        </div>
      </div>
    </div>
  </section>
</template>

<style scoped>
.settings-view{max-width:1120px;margin:0 auto;padding:46px 44px 80px;color:#24313b}.settings-header,.section-heading,.local-card,.form-actions{display:flex;align-items:center;justify-content:space-between;gap:24px}.settings-eyebrow{margin:0;color:#168866;font-size:12px;font-weight:800;letter-spacing:.12em}.settings-header h1{margin:7px 0 10px;font-size:32px}.settings-header p,.empty-copy{color:#687586}.runtime-badge,.provider-state{padding:8px 12px;border:1px solid #dce3e5;border-radius:999px;background:#fff;color:#687586;font-size:12px;font-weight:700}.runtime-badge span{display:inline-block;width:7px;height:7px;margin-right:7px;border-radius:50%;background:#168866}.local-card{margin:28px 0;padding:18px 20px;border:1px solid #dfe5e9;border-radius:15px;background:#fff}.local-card div{display:grid;gap:4px}.local-card small,.provider-form label small{color:#7b8794}.local-card>span,.provider-state.ready{color:#168866;font-weight:800}.model-section{padding:26px;border:1px solid #dfe5e9;border-radius:18px;background:#fff}.section-heading{margin-bottom:22px}.section-heading h2{margin:5px 0 0;font-size:23px}.model-grid{display:grid;grid-template-columns:260px minmax(0,1fr);gap:24px}.profile-list{min-height:360px;padding-right:18px;border-right:1px solid #edf0f1}.profile-list button{width:100%;border:0;text-align:left}.new-profile{margin-bottom:10px;padding:11px 12px;border-radius:10px!important;background:#ecf7f3;color:#11785c;font-weight:750}.profile-item{display:flex;align-items:center;justify-content:space-between;margin:5px 0;padding:12px;border-radius:11px;background:transparent;color:#34434f;cursor:pointer}.profile-item.selected{background:#f2f5f4}.profile-item span{display:grid;gap:3px}.profile-item small{max-width:160px;overflow:hidden;color:#7b8794;text-overflow:ellipsis}.profile-item i{color:#168866;font-size:11px;font-style:normal}.provider-form{display:grid;gap:15px}.provider-form label{display:grid;gap:7px;color:#45535e;font-size:13px;font-weight:700}.provider-form input,.provider-form select{box-sizing:border-box;width:100%;padding:11px 12px;border:1px solid #d9e0e3;border-radius:10px;background:#fff;color:#24313b;font:inherit;font-weight:500}.form-row{display:grid;grid-template-columns:1fr 1fr;gap:14px}.form-actions{margin-top:8px}.form-actions button{padding:10px 15px;border:1px solid #d7dfe1;border-radius:10px;background:#fff;color:#41505b;font-weight:750;cursor:pointer}.form-actions button:disabled{cursor:not-allowed;opacity:.5}.form-actions .primary{border-color:#168866;background:#168866;color:#fff}.form-actions .danger{border-color:#f0d6d3;color:#b54b42}.test-result{padding:10px 12px;border-radius:9px;background:#f6f7f8;color:#687586;font-size:13px}.test-result.success{background:#edf8f4;color:#11785c}.test-result.failed{background:#fff3f1;color:#a9463e}@media(max-width:850px){.model-grid{grid-template-columns:1fr}.profile-list{min-height:0;padding:0 0 16px;border-right:0;border-bottom:1px solid #edf0f1}.settings-header{align-items:flex-start;flex-direction:column}.form-row{grid-template-columns:1fr}}
.model-input-row{display:grid;grid-template-columns:minmax(0,1fr) auto;gap:8px}.model-input-row button{padding:0 13px;border:1px solid #d7dfe1;border-radius:10px;background:#f7f9f9;color:#41505b;font-weight:750;white-space:nowrap}
</style>
