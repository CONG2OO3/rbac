<script setup>
import { ref } from 'vue'
import { UserRound, ShieldCheck, Building2, BriefcaseBusiness, Mail, Phone, KeyRound, Clock3, MonitorSmartphone, MapPin, Save, CheckCircle2 } from 'lucide-vue-next'

const tab = ref('password')
const saved = ref(false)
const form = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })
const loginLogs = [
  ['2024-09-26 14:30:22', 'Chrome 128 / Windows 11', '192.168.1.10', '登录成功'],
  ['2024-09-25 09:12:08', 'Chrome 128 / Windows 11', '192.168.1.10', '登录成功'],
  ['2024-09-24 18:42:35', 'Safari 17 / macOS', '192.168.1.32', '登录成功'],
  ['2024-09-23 08:56:14', 'Chrome 128 / Windows 11', '192.168.1.10', '登录成功'],
]
function savePassword() {
  saved.value = true
  window.setTimeout(() => { saved.value = false }, 2200)
}
</script>

<template>
  <div class="profile-page"><div class="page-intro"><div><h1>个人中心</h1><p>管理个人资料、安全设置和登录记录</p></div></div><div class="profile-layout"><section class="profile-card panel"><div class="profile-cover"></div><div class="profile-avatar">张</div><h2>张三</h2><p class="profile-role">超级管理员</p><div class="profile-status"><i></i>账号正常</div><div class="profile-info"><div><UserRound :size="16" /><span>账号</span><b>admin</b></div><div><Building2 :size="16" /><span>所属部门</span><b>技术研发部</b></div><div><BriefcaseBusiness :size="16" /><span>岗位</span><b>高级研发工程师</b></div><div><Mail :size="16" /><span>邮箱</span><b>admin@enterprise.com</b></div><div><Phone :size="16" /><span>手机号</span><b>138****8001</b></div><div><Clock3 :size="16" /><span>注册时间</span><b>2024-01-01</b></div></div></section><section class="profile-main panel"><div class="profile-tabs"><button :class="{ active: tab === 'password' }" @click="tab = 'password'"><KeyRound :size="17" />修改密码</button><button :class="{ active: tab === 'logs' }" @click="tab = 'logs'"><MonitorSmartphone :size="17" />登录日志</button></div><div v-if="tab === 'password'" class="password-panel"><div class="panel-title"><h2>修改密码</h2><p>定期修改密码可以提升账号安全性</p></div><div class="password-form"><label><span>当前密码</span><input v-model="form.oldPassword" type="password" placeholder="请输入当前密码" /></label><label><span>新密码</span><input v-model="form.newPassword" type="password" placeholder="请输入新密码，至少 6 位" /></label><label><span>确认新密码</span><input v-model="form.confirmPassword" type="password" placeholder="请再次输入新密码" /></label><div class="password-tip"><ShieldCheck :size="17" /><span>密码需至少包含 6 个字符，建议使用数字、字母和符号的组合。</span></div><button class="primary-btn save-password" @click="savePassword"><Save :size="16" />保存密码</button><div v-if="saved" class="saved-inline"><CheckCircle2 :size="16" />密码修改成功</div></div></div><div v-else class="logs-panel"><div class="panel-title"><h2>登录日志</h2><p>记录最近的账号登录行为</p></div><div class="login-log-table"><div class="log-row log-head"><span>登录时间</span><span>登录设备</span><span>登录 IP</span><span>登录结果</span></div><div v-for="log in loginLogs" :key="log[0]" class="log-row"><span>{{ log[0] }}</span><span>{{ log[1] }}</span><span>{{ log[2] }}</span><span class="log-success"><i></i>{{ log[3] }}</span></div></div></div></section></div></div>
</template>
