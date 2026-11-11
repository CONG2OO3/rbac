<script setup>
import { onMounted, onUnmounted, ref, watch } from 'vue'
import { Eye, EyeOff, ShieldCheck, ArrowRight, CheckCircle2, RefreshCw } from 'lucide-vue-next'
import { useRoute, useRouter } from 'vue-router'
import request from '../api/request'
import { getCaptcha, loadPublicConfig, platformConfig } from '../api/platformConfig'
import '../styles/config.css'

const router = useRouter()
const route = useRoute()
const username = ref('')
const password = ref('')
const remember = ref(true)
const showPassword = ref(false)
const loading = ref(false)
const error = ref('')
const captcha = ref(null)
const captchaCode = ref('')

async function refreshCaptcha() {
  captchaCode.value = ''
  try {
    captcha.value = await getCaptcha()
  } catch (exception) {
    captcha.value = null
    error.value = exception.message || '验证码加载失败'
  }
}

async function submit() {
  error.value = ''
  if (!username.value.trim() || !password.value) {
    error.value = '请输入账号和密码'
    return
  }
  if (platformConfig.captchaEnabled && !captchaCode.value.trim()) {
    error.value = '请输入验证码'
    return
  }
  loading.value = true
  try {
    const data = await request.post('/auth/login', {
      username: username.value.trim(),
      password: password.value,
      captchaId: captcha.value?.id,
      captchaCode: captchaCode.value.trim(),
    })
    localStorage.setItem('access_token', data.token)
    if (remember.value) localStorage.setItem('remember_login', '1')
    else localStorage.removeItem('remember_login')
    router.replace(typeof route.query.redirect === 'string' ? route.query.redirect : '/')
  } catch (exception) {
    error.value = exception.message || '登录失败'
    try {
      await loadPublicConfig()
      if (platformConfig.captchaEnabled) await refreshCaptcha()
    } catch {
      // 保留原有错误提示，允许用户重新尝试。
    }
  } finally {
    loading.value = false
  }
}

async function syncPublicConfig() {
  try {
    await loadPublicConfig()
    if (platformConfig.captchaEnabled && !captcha.value) await refreshCaptcha()
    if (!platformConfig.captchaEnabled) captcha.value = null
  } catch (exception) {
    error.value = exception.message || '系统配置加载失败'
  }
}

onMounted(() => {
  window.addEventListener('focus', syncPublicConfig)
  syncPublicConfig()
})
onUnmounted(() => window.removeEventListener('focus', syncPublicConfig))
watch(() => platformConfig.systemName, (name) => {
  document.title = `登录 - ${name}`
})
</script>

<template>
  <div class="login-page">
    <div class="login-decoration decoration-one"></div>
    <div class="login-decoration decoration-two"></div>
    <div class="login-brand">
      <img v-if="platformConfig.logoUrl" class="brand-logo" :src="platformConfig.logoUrl" alt="系统 Logo" />
      <div v-else class="brand-mark"><i></i><i></i><i></i><i></i></div>
      <div><strong>{{ platformConfig.systemName }}</strong><span>{{ platformConfig.systemDescription }}</span></div>
    </div>
    <div class="login-content">
      <section class="login-intro">
        <div class="intro-badge"><ShieldCheck :size="16" />企业数字化管理平台</div>
        <h1>让管理更简单<br /><span>让协作更高效</span></h1>
        <p>统一管理企业资源，连接每一个团队成员，助力企业持续成长。</p>
        <div class="intro-points">
          <span><CheckCircle2 :size="16" />统一身份认证</span>
          <span><CheckCircle2 :size="16" />安全权限控制</span>
          <span><CheckCircle2 :size="16" />实时数据洞察</span>
        </div>
        <svg class="login-illustration" viewBox="0 0 500 260">
          <defs>
            <linearGradient id="lg" x1="0" x2="1"><stop stop-color="#dceaff"/><stop offset="1" stop-color="#a8caff"/></linearGradient>
            <linearGradient id="ls" x1="0" y1="0" x2="1" y2="1"><stop stop-color="#82b1ff"/><stop offset="1" stop-color="#1d6df2"/></linearGradient>
          </defs>
          <path d="M42 212 256 102l196 91-205 52z" fill="url(#lg)"/>
          <path d="M149 84 319 7l127 63-173 83z" fill="#eaf3ff"/>
          <path d="M149 84v116l124 52V151z" fill="#fff"/>
          <path d="m273 151 173-81v107l-173 75z" fill="#8eb9ff"/>
          <path d="m171 105 72-34v93l-72 35z" fill="url(#ls)"/>
          <path d="m184 123 38-18v8l-38 18zm0 20 51-24v8l-51 25zm0 20 34-16v8l-34 16z" fill="#fff" opacity=".9"/>
          <path d="m296 55 37-17 43 22-37 17z" fill="#2474f4"/>
          <path d="M296 55v72l43 22V77z" fill="#fff"/>
          <path d="m339 77 37-17v72l-37 17z" fill="#d6e6ff"/>
          <circle cx="103" cy="92" r="6" fill="#78a9f9"/>
          <circle cx="426" cy="174" r="5" fill="#78a9f9"/>
        </svg>
      </section>
      <section class="login-card">
        <div class="login-card-head"><h2>欢迎登录</h2><p>登录您的企业管理账户</p></div>
        <form @submit.prevent="submit">
          <label>账号<input v-model="username" autocomplete="username" placeholder="请输入账号" /></label>
          <label>密码
            <div class="password-input">
              <input v-model="password" :type="showPassword ? 'text' : 'password'" autocomplete="current-password" placeholder="请输入密码" />
              <button type="button" aria-label="切换密码可见性" @click="showPassword = !showPassword">
                <EyeOff v-if="showPassword" :size="17" /><Eye v-else :size="17" />
              </button>
            </div>
          </label>
          <label v-if="platformConfig.captchaEnabled">验证码
            <div class="captcha-row">
              <input v-model="captchaCode" maxlength="4" autocomplete="off" placeholder="请输入验证码" />
              <button type="button" class="captcha-image" title="点击刷新验证码" @click="refreshCaptcha">
                <img v-if="captcha?.image" :src="captcha.image" alt="图形验证码" />
                <RefreshCw v-else :size="18" />
              </button>
            </div>
          </label>
          <div class="login-options">
            <label class="remember"><input v-model="remember" type="checkbox" />记住我</label>
            <router-link v-if="platformConfig.registrationEnabled" to="/register">注册账号</router-link>
          </div>
          <div v-if="error" class="login-error">{{ error }}</div>
          <button class="login-submit" :disabled="loading">{{ loading ? '登录中...' : '登 录' }}<ArrowRight :size="17" /></button>
        </form>
      </section>
    </div>
    <div class="login-footer">© 2024 {{ platformConfig.systemName }}　·　安全登录中心</div>
  </div>
</template>
