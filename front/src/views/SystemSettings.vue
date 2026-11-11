<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Database, ImagePlus, Save, ShieldCheck, UserPlus, X } from 'lucide-vue-next'
import {
  clearSystemLogo,
  loadPublicConfig,
  loadStorageConfig,
  platformConfig,
  saveLoginConfig,
  saveRegistrationConfig,
  saveSystemConfig,
  saveStorageConfig,
  uploadSystemLogo,
} from '../api/platformConfig'
import '../styles/config.css'

const tab = ref('system')
const loading = ref(true)
const storageLoading = ref(true)
const saving = ref(false)
const error = ref('')
const success = ref('')
const storageError = ref('')
const logoInput = ref(null)
const form = reactive({
  systemName: '',
  systemDescription: '',
  captchaEnabled: false,
  registrationEnabled: false,
})
const activeStorage = ref('minio')
const storageForms = reactive({
  aliyun: { endpoint: '', bucket: '', accessKeyId: '', accessKeySecret: '', secretConfigured: false },
  minio: { endpoint: '', bucket: '', accessKey: '', secretKey: '', secretConfigured: false },
  starimg: { endpoint: 'https://starimg.vip/api/upload', apiKey: '', apiKeyConfigured: false },
})

function fillStorage(value) {
  activeStorage.value = value.activeType
  for (const type of ['aliyun', 'minio', 'starimg']) {
    Object.assign(storageForms[type], value[type])
  }
  storageForms.aliyun.accessKeySecret = ''
  storageForms.minio.secretKey = ''
  storageForms.starimg.apiKey = ''
}

function fillForm(config) {
  form.systemName = config.systemName
  form.systemDescription = config.systemDescription
  form.captchaEnabled = config.captchaEnabled
  form.registrationEnabled = config.registrationEnabled
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    fillForm(await loadPublicConfig())
  } catch (exception) {
    error.value = exception.message || '配置加载失败'
  } finally {
    loading.value = false
  }
  try {
    fillStorage(await loadStorageConfig())
    storageError.value = ''
  } catch (exception) {
    storageError.value = exception.message || '存储配置加载失败'
  } finally {
    storageLoading.value = false
  }
}

async function save() {
  if (saving.value) return
  error.value = ''
  success.value = ''
  saving.value = true
  try {
    let result
    if (tab.value === 'system') {
      result = await saveSystemConfig({
        systemName: form.systemName.trim(),
        systemDescription: form.systemDescription.trim(),
      })
    } else if (tab.value === 'login') {
      result = await saveLoginConfig(form.captchaEnabled)
    } else if (tab.value === 'register') {
      result = await saveRegistrationConfig(form.registrationEnabled)
    } else {
      fillStorage(await saveStorageConfig({
        type: activeStorage.value,
        ...storageForms[activeStorage.value],
      }))
    }
    if (result) fillForm(result)
    success.value = '配置已保存并立即生效'
  } catch (exception) {
    error.value = exception.message || '保存失败'
    if (tab.value === 'login') form.captchaEnabled = platformConfig.captchaEnabled
    if (tab.value === 'register') form.registrationEnabled = platformConfig.registrationEnabled
  } finally {
    saving.value = false
  }
}

async function uploadLogo(event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return
  error.value = ''
  success.value = ''
  if (!file.type.startsWith('image/') || file.size > 2 * 1024 * 1024) {
    error.value = '请选择不超过 2MB 的图片'
    return
  }
  saving.value = true
  try {
    fillForm(await uploadSystemLogo(file))
    success.value = 'Logo 已更新'
  } catch (exception) {
    error.value = exception.message || 'Logo 上传失败'
  } finally {
    saving.value = false
  }
}

async function removeLogo() {
  error.value = ''
  success.value = ''
  saving.value = true
  try {
    fillForm(await clearSystemLogo())
    success.value = 'Logo 已移除'
  } catch (exception) {
    error.value = exception.message || '移除 Logo 失败'
  } finally {
    saving.value = false
  }
}

function selectTab(value) {
  tab.value = value
  error.value = ''
  success.value = ''
}

onMounted(load)
</script>

<template>
  <div class="config-page">
    <div class="page-intro">
      <div>
        <h1>系统配置</h1>
        <p>统一管理平台信息、登录验证、用户注册和文件存储</p>
      </div>
      <button class="primary-btn" :disabled="loading || saving || (tab === 'storage' && storageLoading)" @click="save">
        <Save :size="16" />{{ saving ? '保存中...' : '保存配置' }}
      </button>
    </div>

    <section class="panel config-panel">
      <div class="config-tabs" role="tablist" aria-label="系统配置类别">
        <button role="tab" :aria-selected="tab === 'system'" :class="{ active: tab === 'system' }" @click="selectTab('system')">
          <ImagePlus :size="17" />系统配置
        </button>
        <button role="tab" :aria-selected="tab === 'login'" :class="{ active: tab === 'login' }" @click="selectTab('login')">
          <ShieldCheck :size="17" />登录配置
        </button>
        <button role="tab" :aria-selected="tab === 'register'" :class="{ active: tab === 'register' }" @click="selectTab('register')">
          <UserPlus :size="17" />注册配置
        </button>
        <button role="tab" :aria-selected="tab === 'storage'" :class="{ active: tab === 'storage' }" @click="selectTab('storage')">
          <Database :size="17" />存储配置
        </button>
      </div>

      <div class="config-body">
        <div v-if="loading" class="config-loading">正在加载系统配置...</div>
        <template v-else>
          <div v-if="error" class="api-error">{{ error }}</div>
          <div v-if="tab === 'storage' && storageError" class="api-error">{{ storageError }}</div>
          <div v-if="success" class="config-success">{{ success }}</div>

          <div v-if="tab === 'system'" class="config-fields">
            <div class="config-section-head">
              <h2>系统信息</h2>
              <p>名称与 Logo 将显示在导航栏和登录页，保存后立即生效。</p>
            </div>
            <label class="config-field">
              <span>系统名称 <em>*</em></span>
              <input v-model="form.systemName" maxlength="60" placeholder="请输入系统名称" />
            </label>
            <label class="config-field">
              <span>系统描述</span>
              <textarea v-model="form.systemDescription" maxlength="500" rows="3" placeholder="简要描述系统用途"></textarea>
            </label>
            <div class="config-field">
              <span>系统 Logo</span>
              <div class="config-logo-row">
                <div class="config-logo-preview">
                  <img v-if="platformConfig.logoUrl" :src="platformConfig.logoUrl" alt="当前系统 Logo" />
                  <ImagePlus v-else :size="26" />
                </div>
                <div>
                  <input ref="logoInput" class="config-file-input" type="file" accept="image/*" @change="uploadLogo" />
                  <button class="config-outline-btn" :disabled="saving" @click="logoInput?.click()">上传 Logo</button>
                  <button v-if="platformConfig.logoUrl" class="config-text-btn" :disabled="saving" @click="removeLogo"><X :size="15" />移除</button>
                  <p class="config-help">支持图片格式，最大 2MB。上传后会立即更新。</p>
                </div>
              </div>
            </div>
          </div>

          <div v-else-if="tab === 'login'" class="config-fields">
            <div class="config-section-head">
              <h2>登录配置</h2>
              <p>控制登录时的身份验证方式。</p>
            </div>
            <div class="config-switch-row">
              <div>
                <strong>开启图形验证码</strong>
                <p>开启后，登录和注册都必须填写一次性验证码。验证码 5 分钟内有效。</p>
              </div>
              <label class="config-toggle">
                <input v-model="form.captchaEnabled" type="checkbox" :disabled="saving" @change="save" />
                <span></span>
              </label>
            </div>
          </div>

          <div v-else-if="tab === 'register'" class="config-fields">
            <div class="config-section-head">
              <h2>注册配置</h2>
              <p>控制是否开放普通用户自主注册。</p>
            </div>
            <div class="config-switch-row">
              <div>
                <strong>允许用户注册</strong>
                <p>开启后登录页显示注册入口，新用户自动获得普通用户角色。</p>
              </div>
              <label class="config-toggle">
                <input v-model="form.registrationEnabled" type="checkbox" :disabled="saving" @change="save" />
                <span></span>
              </label>
            </div>
          </div>

          <div v-else class="config-fields storage-fields">
            <div class="config-section-head">
              <h2>文件存储</h2>
              <p>保存后立即切换新上传文件的存储方式。历史文件仍使用原存储服务，请保留对应配置。</p>
            </div>
            <div class="storage-provider-list" role="radiogroup" aria-label="当前存储服务">
              <label v-for="provider in [
                { type: 'aliyun', name: '阿里云 OSS', detail: '对象存储服务' },
                { type: 'minio', name: 'MinIO', detail: '自建对象存储' },
                { type: 'starimg', name: '星云图', detail: '仅支持图片上传' },
              ]" :key="provider.type" class="storage-provider" :class="{ selected: activeStorage === provider.type }">
                <input v-model="activeStorage" type="radio" :value="provider.type" />
                <span><strong>{{ provider.name }}</strong><small>{{ provider.detail }}</small></span>
              </label>
            </div>

            <template v-if="activeStorage === 'aliyun'">
              <label class="config-field"><span>Endpoint <em>*</em></span><input v-model.trim="storageForms.aliyun.endpoint" placeholder="https://oss-cn-beijing.aliyuncs.com" /></label>
              <label class="config-field"><span>Bucket 名称 <em>*</em></span><input v-model.trim="storageForms.aliyun.bucket" placeholder="请输入 Bucket 名称" /></label>
              <label class="config-field"><span>AccessKey ID <em>*</em></span><input v-model.trim="storageForms.aliyun.accessKeyId" autocomplete="off" placeholder="请输入 AccessKey ID" /></label>
              <label class="config-field"><span>AccessKey Secret <em>*</em></span><input v-model="storageForms.aliyun.accessKeySecret" type="password" autocomplete="new-password" :placeholder="storageForms.aliyun.secretConfigured ? '已配置，留空则保持不变' : '请输入 AccessKey Secret'" /></label>
            </template>
            <template v-else-if="activeStorage === 'minio'">
              <label class="config-field"><span>Endpoint <em>*</em></span><input v-model.trim="storageForms.minio.endpoint" placeholder="https://minio.example.com" /></label>
              <label class="config-field"><span>Bucket 名称 <em>*</em></span><input v-model.trim="storageForms.minio.bucket" placeholder="请输入 Bucket 名称" /></label>
              <label class="config-field"><span>Access Key <em>*</em></span><input v-model.trim="storageForms.minio.accessKey" autocomplete="off" placeholder="请输入 Access Key" /></label>
              <label class="config-field"><span>Secret Key <em>*</em></span><input v-model="storageForms.minio.secretKey" type="password" autocomplete="new-password" :placeholder="storageForms.minio.secretConfigured ? '已配置，留空则保持不变' : '请输入 Secret Key'" /></label>
            </template>
            <template v-else>
              <label class="config-field"><span>上传地址 <em>*</em></span><input v-model.trim="storageForms.starimg.endpoint" placeholder="https://starimg.vip/api/upload" /></label>
              <label class="config-field"><span>X-API-Key <em>*</em></span><input v-model="storageForms.starimg.apiKey" type="password" autocomplete="new-password" :placeholder="storageForms.starimg.apiKeyConfigured ? '已配置，留空则保持不变' : '请输入 API Key'" /></label>
              <p class="config-help">星云图未提供删除接口；删除系统记录不会删除图床中的图片。</p>
            </template>
          </div>
        </template>
      </div>
    </section>
  </div>
</template>
