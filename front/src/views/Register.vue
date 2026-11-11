<script setup>
import { onMounted, onUnmounted, reactive, ref, watch } from 'vue'
import { ArrowLeft, RefreshCw, UserPlus } from 'lucide-vue-next'
import {
  getCaptcha,
  loadPublicConfig,
  platformConfig,
  registerUser,
} from '../api/platformConfig'
import '../styles/config.css'

const form = reactive({
  username: '',
  nickName: '',
  password: '',
  confirmPassword: '',
  captchaCode: '',
})
const captcha = ref(null)
const loading = ref(true)
const saving = ref(false)
const success = ref(false)
const error = ref('')

async function refreshCaptcha() {
  form.captchaCode = ''
  try {
    captcha.value = await getCaptcha()
  } catch (exception) {
    captcha.value = null
    error.value = exception.message || '验证码加载失败'
  }
}

async function submit() {
  error.value = ''
  if (!platformConfig.registrationEnabled) {
    error.value = '当前系统未开放用户注册'
    return
  }
  if (form.password !== form.confirmPassword) {
    error.value = '两次输入的密码不一致'
    return
  }
  if (platformConfig.captchaEnabled && !form.captchaCode.trim()) {
    error.value = '请输入验证码'
    return
  }
  saving.value = true
  try {
    await registerUser({
      ...form,
      username: form.username.trim(),
      nickName: form.nickName.trim(),
      captchaId: captcha.value?.id,
      captchaCode: form.captchaCode.trim(),
    })
    success.value = true
  } catch (exception) {
    error.value = exception.message || '注册失败'
    try {
      await loadPublicConfig()
      if (platformConfig.captchaEnabled) await refreshCaptcha()
    } catch {
      // 保留服务端的注册错误信息。
    }
  } finally {
    saving.value = false
  }
}

async function syncPublicConfig() {
  try {
    await loadPublicConfig()
    if (platformConfig.registrationEnabled && platformConfig.captchaEnabled && !captcha.value) {
      await refreshCaptcha()
    }
    if (!platformConfig.captchaEnabled) captcha.value = null
  } catch (exception) {
    error.value = exception.message || '系统配置加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  window.addEventListener('focus', syncPublicConfig)
  syncPublicConfig()
})
onUnmounted(() => window.removeEventListener('focus', syncPublicConfig))
watch(() => platformConfig.systemName, (name) => {
  document.title = `注册 - ${name}`
})
</script>

<template>
  <div class="login-page register-page">
    <div class="login-decoration decoration-one"></div>
    <div class="login-decoration decoration-two"></div>
    <div class="login-brand">
      <img v-if="platformConfig.logoUrl" class="brand-logo" :src="platformConfig.logoUrl" alt="系统 Logo" />
      <div v-else class="brand-mark"><i></i><i></i><i></i><i></i></div>
      <div><strong>{{ platformConfig.systemName }}</strong><span>{{ platformConfig.systemDescription }}</span></div>
    </div>
    <section class="login-card register-card">
      <div class="register-heading"><UserPlus :size="25" /><h2>创建账号</h2></div>
      <p class="register-subtitle">注册后即可使用普通用户账号登录</p>

      <div v-if="loading" class="config-loading">正在加载注册配置...</div>
      <div v-else-if="!platformConfig.registrationEnabled" class="register-unavailable">
        当前系统未开放用户注册，请联系管理员。
      </div>
      <div v-else-if="success" class="register-done">
        <h3>注册成功</h3>
        <p>账号已创建，请返回登录页使用新账号登录。</p>
      </div>
      <form v-else @submit.prevent="submit">
        <label>账号<input v-model="form.username" minlength="4" maxlength="30" autocomplete="username" placeholder="4-30 位字母、数字或下划线" required /></label>
        <label>用户昵称<input v-model="form.nickName" maxlength="30" placeholder="请输入用户昵称" required /></label>
        <label>密码<input v-model="form.password" type="password" minlength="6" maxlength="72" autocomplete="new-password" placeholder="至少 6 位" required /></label>
        <label>确认密码<input v-model="form.confirmPassword" type="password" minlength="6" maxlength="72" autocomplete="new-password" placeholder="再次输入密码" required /></label>
        <label v-if="platformConfig.captchaEnabled">验证码
          <div class="captcha-row">
            <input v-model="form.captchaCode" maxlength="4" autocomplete="off" placeholder="请输入验证码" />
            <button type="button" class="captcha-image" title="点击刷新验证码" @click="refreshCaptcha">
              <img v-if="captcha?.image" :src="captcha.image" alt="图形验证码" />
              <RefreshCw v-else :size="18" />
            </button>
          </div>
        </label>
        <div v-if="error" class="login-error">{{ error }}</div>
        <button class="login-submit" :disabled="saving">{{ saving ? '提交中...' : '注 册' }}</button>
      </form>
      <router-link class="register-back" to="/login"><ArrowLeft :size="15" />返回登录</router-link>
    </section>
    <div class="login-footer">© 2024 {{ platformConfig.systemName }}　·　安全注册</div>
  </div>
</template>
