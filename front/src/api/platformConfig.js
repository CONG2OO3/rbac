import { reactive } from 'vue'
import request from './request'

export const platformConfig = reactive({
  systemName: '企业级管理系统',
  systemDescription: '高效 · 安全 · 可扩展',
  logoUrl: '',
  captchaEnabled: false,
  registrationEnabled: false,
})

export function applyPlatformConfig(value) {
  Object.assign(platformConfig, value)
  return platformConfig
}

export async function loadPublicConfig() {
  return applyPlatformConfig(await request.get('/public/config', { params: { _t: Date.now() } }))
}

export async function saveSystemConfig(data) {
  return applyPlatformConfig(await request.put('/system/config/system', data))
}

export async function saveLoginConfig(enabled) {
  return applyPlatformConfig(await request.put('/system/config/login', { enabled }))
}

export async function saveRegistrationConfig(enabled) {
  return applyPlatformConfig(await request.put('/system/config/register', { enabled }))
}

export async function uploadSystemLogo(file) {
  const form = new FormData()
  form.append('file', file)
  return applyPlatformConfig(await request.post('/system/config/logo', form))
}

export async function clearSystemLogo() {
  return applyPlatformConfig(await request.delete('/system/config/logo'))
}

export function loadStorageConfig() {
  return request.get('/system/config/storage')
}

export function saveStorageConfig(data) {
  return request.put('/system/config/storage', data)
}

export function getCaptcha() {
  return request.get('/auth/captcha')
}

export function registerUser(data) {
  return request.post('/auth/register', data)
}
