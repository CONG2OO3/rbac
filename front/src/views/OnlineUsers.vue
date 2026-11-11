<script setup>
import { onMounted, onUnmounted, ref, watch } from 'vue'
import { ChevronLeft, ChevronRight, LogOut, RefreshCw, Search, Users } from 'lucide-vue-next'
import { kickoutOnlineUser, listOnlineUsers } from '../api/monitor'

const keyword = ref('')
const appliedKeyword = ref('')
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const rows = ref([])
const loading = ref(false)
const error = ref('')
const kicking = ref('')
let pollTimer

function formatTime(value) {
  return value ? value.replace('T', ' ') : '-'
}

async function loadRows(silent = false) {
  if (!silent) loading.value = true
  try {
    const result = await listOnlineUsers({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: appliedKeyword.value || undefined,
    })
    rows.value = result.records || []
    total.value = result.total || 0
    error.value = ''
    if (pageNum.value > 1 && rows.value.length === 0) pageNum.value--
  } catch (exception) {
    error.value = exception.message || '在线用户加载失败'
    if (!silent) {
      rows.value = []
      total.value = 0
    }
  } finally {
    loading.value = false
  }
}

function searchUsers() {
  appliedKeyword.value = keyword.value.trim()
  pageNum.value = 1
  loadRows()
}

async function forceLogout(item) {
  if (!window.confirm(`确认强制下线“${item.nickName}”的这个登录会话吗？`)) return
  kicking.value = item.sessionId
  error.value = ''
  try {
    await kickoutOnlineUser(item.sessionId)
    await loadRows()
  } catch (exception) {
    error.value = exception.message || '强制下线失败'
  } finally {
    kicking.value = ''
  }
}

watch([pageNum, pageSize], () => loadRows())
onMounted(() => {
  loadRows()
  pollTimer = window.setInterval(() => loadRows(true), 15000)
})
onUnmounted(() => window.clearInterval(pollTimer))
</script>

<template>
  <div class="online-page">
    <div class="page-intro"><div><h1>在线用户</h1><p>查看当前有效登录会话与用户活跃情况</p></div></div>
    <section class="panel online-panel">
      <div class="filter-bar online-filter">
        <div class="search-field"><Search :size="17" /><input v-model="keyword" placeholder="请输入账号、昵称或 IP" @keyup.enter="searchUsers" /></div>
        <button class="filter-btn" @click="searchUsers"><Search :size="16" />查询</button>
        <span class="filter-spacer"></span>
        <span class="auto-tip">每 15 秒自动刷新</span>
        <button class="plain-icon" title="刷新" @click="loadRows()"><RefreshCw :size="17" /></button>
      </div>
      <div v-if="error" class="api-error">{{ error }}</div>
      <div class="online-table">
        <div class="online-row online-head"><span>用户</span><span>登录账号</span><span>登录 IP</span><span>浏览器 / 系统</span><span>登录时间</span><span>最近活跃</span><span>操作</span></div>
        <div v-if="loading" class="online-empty">正在加载在线用户...</div>
        <template v-else>
          <div v-for="item in rows" :key="item.sessionId" class="online-row">
            <span class="online-name"><i>{{ (item.nickName || item.userName).slice(0, 1) }}</i><span>{{ item.nickName }}<small v-if="item.currentSession">当前会话</small></span></span>
            <span>{{ item.userName }}</span>
            <span>{{ item.ipAddress || '-' }}</span>
            <span>{{ item.browser }} / {{ item.operatingSystem }}</span>
            <span>{{ formatTime(item.loginTime) }}</span>
            <span>{{ formatTime(item.lastActiveTime) }}</span>
            <span><button class="kickout-btn" :disabled="item.currentSession || kicking === item.sessionId" :title="item.currentSession ? '不能下线当前会话' : '强制下线'" @click="forceLogout(item)"><LogOut :size="15" />下线</button></span>
          </div>
          <div v-if="!rows.length" class="online-empty"><Users :size="34" /><strong>暂无在线用户</strong><span>可尝试清空搜索条件后刷新</span></div>
        </template>
      </div>
      <div class="table-footer"><span>共 {{ total }} 个会话</span><div class="pagination"><button :disabled="pageNum <= 1" @click="pageNum--"><ChevronLeft :size="15" /></button><button class="current">{{ pageNum }}</button><button :disabled="pageNum * pageSize >= total" @click="pageNum++"><ChevronRight :size="15" /></button></div><select v-model.number="pageSize"><option :value="10">10 条/页</option><option :value="20">20 条/页</option><option :value="50">50 条/页</option></select></div>
    </section>
  </div>
</template>

<style scoped>
.online-panel{min-height:530px;padding:18px;display:flex;flex-direction:column}.online-filter{margin:-18px -18px 18px;padding:11px 18px;border-bottom:1px solid #edf1f7;min-height:58px}.auto-tip{color:#9aa9bc;font-size:12px}.online-table{border:1px solid #e8eef6;border-radius:6px;overflow:auto}.online-row{min-width:1120px;min-height:58px;display:grid;grid-template-columns:1.1fr .85fr .85fr 1.2fr 1.1fr 1.1fr 84px;align-items:center;gap:8px;padding:0 15px;border-bottom:1px solid #edf1f7;color:#526b8a;font-size:12px}.online-row:last-child{border-bottom:0}.online-row>span{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.online-head{min-height:42px;color:#466080;background:#f7f9fc;font-weight:600}.online-name{display:flex;align-items:center;gap:9px}.online-name i{width:30px;height:30px;flex:none;display:grid;place-items:center;border-radius:50%;color:#176cff;background:#e8f1ff;font-style:normal;font-weight:600}.online-name>span{overflow:hidden;text-overflow:ellipsis}.online-name small{margin-left:7px;padding:2px 5px;border-radius:4px;color:#0aab76;background:#e6f8f0;font-size:10px}.kickout-btn{display:inline-flex;align-items:center;gap:4px;padding:6px;color:#e2535c;font-size:12px}.kickout-btn:hover{background:#fff1f1;border-radius:5px}.kickout-btn:disabled{color:#adb9c8;cursor:not-allowed}.online-empty{min-height:250px;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:10px;color:#9badc3;font-size:13px}.online-empty strong{color:#55708f;font-size:15px}.online-empty span{font-size:12px}.table-footer{margin-top:auto}:global(:root[data-theme="dark"]) .online-filter,:global(:root[data-theme="dark"]) .online-table,:global(:root[data-theme="dark"]) .online-row{border-color:#2c2c32}:global(:root[data-theme="dark"]) .online-head{background:#222226;color:#c3cedc}:global(:root[data-theme="dark"]) .online-row{color:#b0bdce}:global(:root[data-theme="dark"]) .online-name i{background:#203d68;color:#83b4ff}:global(:root[data-theme="dark"]) .online-name small{background:#17382d}:global(:root[data-theme="dark"]) .online-empty strong{color:#d2ddeb}
</style>
