<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ChevronLeft,
  ChevronRight,
  Eye,
  RefreshCw,
  Search,
  Trash2,
  X,
} from 'lucide-vue-next'
import { clearLogs, deleteLogs, listLogs } from '../api/log'

const route = useRoute()
const router = useRouter()
const type = computed(() => route.params.type === 'operation' ? 'operation' : 'login')
const title = computed(() => type.value === 'login' ? '登录日志' : '操作日志')
const keyword = ref('')
const status = ref('')
const pageNum = ref(1)
const pageSize = ref(10)
const total = ref(0)
const rows = ref([])
const selectedIds = ref([])
const loading = ref(false)
const error = ref('')
const detail = ref(null)

const columns = computed(() => type.value === 'login'
  ? [
      ['userName', '登录账号'],
      ['ipAddress', '登录 IP'],
      ['browser', '浏览器'],
      ['operatingSystem', '操作系统'],
      ['message', '登录结果'],
      ['loginTime', '登录时间'],
    ]
  : [
      ['operatorName', '操作人员'],
      ['module', '操作模块'],
      ['operation', '操作内容'],
      ['requestMethod', '请求方式'],
      ['ipAddress', '操作 IP'],
      ['costTime', '耗时'],
      ['operationTime', '操作时间'],
    ])

const allSelected = computed(() => rows.value.length > 0
  && rows.value.every((item) => selectedIds.value.includes(item.id)))

function formatTime(value) {
  return value ? value.replace('T', ' ') : '-'
}

function cellValue(item, key) {
  if (key.endsWith('Time') && key !== 'costTime') return formatTime(item[key])
  if (key === 'costTime') return `${item[key] ?? 0} ms`
  return item[key] || '-'
}

async function loadRows() {
  loading.value = true
  error.value = ''
  selectedIds.value = []
  try {
    const result = await listLogs(type.value, {
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
      status: status.value || undefined,
    })
    rows.value = result.records || []
    total.value = result.total || 0
  } catch (exception) {
    error.value = exception.message || '日志加载失败'
    rows.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function searchLogs() {
  pageNum.value = 1
  loadRows()
}

function toggleAll(event) {
  selectedIds.value = event.target.checked ? rows.value.map((item) => item.id) : []
}

function toggleRow(id) {
  const index = selectedIds.value.indexOf(id)
  if (index >= 0) selectedIds.value.splice(index, 1)
  else selectedIds.value.push(id)
}

async function removeSelected() {
  if (!selectedIds.value.length) return
  if (!window.confirm(`确认删除选中的 ${selectedIds.value.length} 条日志吗？`)) return
  try {
    await deleteLogs(type.value, selectedIds.value)
    await loadRows()
  } catch (exception) {
    error.value = exception.message || '删除失败'
  }
}

async function clearAll() {
  if (!window.confirm(`确认清空全部${title.value}吗？该操作不可恢复。`)) return
  try {
    await clearLogs(type.value)
    pageNum.value = 1
    await loadRows()
  } catch (exception) {
    error.value = exception.message || '清空失败'
  }
}

watch(
  () => [type.value, pageNum.value, pageSize.value],
  loadRows,
  { immediate: true },
)
</script>

<template>
  <div class="log-page">
    <div class="page-intro">
      <div>
        <h1>日志管理</h1>
        <p>审计系统登录行为与后台操作记录</p>
      </div>
    </div>

    <section class="panel log-panel">
      <div class="log-tabs">
        <button
          :class="{ active: type === 'login' }"
          @click="router.push('/logs/login')"
        >登录日志</button>
        <button
          :class="{ active: type === 'operation' }"
          @click="router.push('/logs/operation')"
        >操作日志</button>
      </div>

      <div class="filter-bar log-filter">
        <div class="search-field">
          <Search :size="17" />
          <input
            v-model="keyword"
            :placeholder="type === 'login' ? '请输入登录账号' : '请输入人员、模块或操作内容'"
            @keyup.enter="searchLogs"
          />
        </div>
        <select v-model="status">
          <option value="">全部状态</option>
          <option value="0">成功</option>
          <option value="1">失败</option>
        </select>
        <button class="filter-btn" @click="searchLogs">
          <Search :size="16" />查询
        </button>
        <span class="filter-spacer"></span>
        <button class="danger-outline" :disabled="!selectedIds.length" @click="removeSelected">
          <Trash2 :size="16" />删除
        </button>
        <button class="danger-outline" @click="clearAll">清空日志</button>
        <button class="plain-icon" title="刷新" @click="loadRows">
          <RefreshCw :size="17" />
        </button>
      </div>

      <div v-if="error" class="api-error">{{ error }}</div>
      <div class="log-table">
        <div
          class="log-data-row log-data-head"
          :style="{ '--columns': columns.length }"
        >
          <span><input type="checkbox" :checked="allSelected" @change="toggleAll" /></span>
          <span v-for="column in columns" :key="column[0]">{{ column[1] }}</span>
          <span>状态</span>
          <span>操作</span>
        </div>
        <div v-if="loading" class="empty-state">正在加载日志...</div>
        <template v-else>
          <div
            v-for="item in rows"
            :key="item.id"
            class="log-data-row"
            :style="{ '--columns': columns.length }"
          >
            <span>
              <input
                type="checkbox"
                :checked="selectedIds.includes(item.id)"
                @change="toggleRow(item.id)"
              />
            </span>
            <span v-for="column in columns" :key="column[0]" :title="cellValue(item, column[0])">
              {{ cellValue(item, column[0]) }}
            </span>
            <span :class="item.status === '0' ? 'log-ok' : 'log-failed'">
              <i></i>{{ item.status === '0' ? '成功' : '失败' }}
            </span>
            <span class="row-actions">
              <button title="查看详情" @click="detail = item"><Eye :size="15" /></button>
            </span>
          </div>
          <div v-if="!rows.length" class="empty-state">暂无日志数据</div>
        </template>
      </div>

      <div class="table-footer">
        <span>共 {{ total }} 条数据</span>
        <div class="pagination">
          <button :disabled="pageNum <= 1" @click="pageNum--"><ChevronLeft :size="15" /></button>
          <button class="current">{{ pageNum }}</button>
          <button :disabled="pageNum * pageSize >= total" @click="pageNum++"><ChevronRight :size="15" /></button>
        </div>
        <select v-model.number="pageSize">
          <option :value="10">10 条/页</option>
          <option :value="20">20 条/页</option>
          <option :value="50">50 条/页</option>
        </select>
      </div>
    </section>

    <div v-if="detail" class="modal-mask" @click.self="detail = null">
      <div class="modal-card log-detail-modal">
        <div class="modal-head">
          <h3>{{ title }}详情</h3>
          <button @click="detail = null"><X :size="19" /></button>
        </div>
        <div class="log-detail-list">
          <template v-for="(value, key) in detail" :key="key">
            <div v-if="key !== 'id'">
              <span>{{ key }}</span>
              <pre>{{ value ?? '-' }}</pre>
            </div>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>
