<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import request from '../api/request'
import { loadPublicConfig, platformConfig } from '../api/platformConfig'
import {
  applyThemeColor,
  DEFAULT_THEME_COLOR,
  getThemeColor,
  normalizeThemeColor,
  THEME_COLOR_PRESETS,
} from '../utils/themeColor'
import '../styles/config.css'
import {
  Bell, Settings, Maximize2, Menu, Search, ChevronDown, ChevronRight,
  House, ShieldCheck, Users, UserRoundCog, ListTree, Network, BriefcaseBusiness,
  FolderKanban, Database, ScrollText, Folder, Paperclip, MonitorCog, PanelLeftClose, Palette, Check,
  LogIn, ClipboardList, Video, Server, UserRoundCheck, Package, ShoppingCart,
} from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const collapsed = ref(false)
const showProfile = ref(false)
const search = ref('')
const openGroups = ref({ '系统管理': true })
const notificationsOpen = ref(false)
const themeOpen = ref(false)
const showTabs = ref(localStorage.getItem('show_tabs') !== '0')
const themeMode = ref(localStorage.getItem('theme_mode') || 'light')
const themeColor = ref(getThemeColor())
const customColorInput = ref(themeColor.value)
const colorError = ref('')
const isAdmin = ref(false)
const profileName = ref('用户')
document.documentElement.dataset.theme = themeMode.value

const allGroups = [
  { label: '首页', path: '/', icon: House },
  { label: '系统管理', icon: ShieldCheck, children: [
    { label: '用户管理', path: '/users', icon: Users },
    { label: '角色管理', path: '/roles', icon: UserRoundCog },
    { label: '菜单管理', path: '/menus', icon: ListTree },
    { label: '部门管理', path: '/departments', icon: Network },
    { label: '岗位管理', path: '/positions', icon: BriefcaseBusiness },
    { label: '系统配置', path: '/settings', icon: Settings },
  ]},
  { label: '业务管理', icon: FolderKanban, children: [
    { label: '产品管理', path: '/business/products', icon: Package },
    { label: '订单管理', path: '/business/orders', icon: ShoppingCart },
  ]},
  { label: '数据管理', path: '/data', icon: Database },
  { label: '日志管理', icon: ScrollText, children: [
    { label: '登录日志', path: '/logs/login', icon: LogIn },
    { label: '操作日志', path: '/logs/operation', icon: ClipboardList },
  ]},
  { label: '文件管理', icon: Folder, children: [
    { label: '图片管理', path: '/images', icon: Folder },
    { label: '附件管理', path: '/attachments', icon: Paperclip },
    { label: '视频管理', path: '/videos', icon: Video },
  ]},
  { label: '系统监控', icon: MonitorCog, children: [
    { label: '服务监控', path: '/monitor/server', icon: Server },
    { label: '在线用户', path: '/monitor/online', icon: UserRoundCheck },
  ]},
]
const groups = computed(() => isAdmin.value ? allGroups : allGroups.slice(0, 1))

const activeLabel = computed(() => route.meta.title || '首页')
const themeOptions = [{ name: '浅色', value: 'light', color: '#f7f9fc' }, { name: '深色', value: 'dark', color: '#171719' }]
const searchableMenus = computed(() => groups.value.flatMap((group) => group.children ? group.children.map((child) => ({ ...child, parent: group.label })) : [{ ...group, parent: '' }]))
const searchResults = computed(() => {
  const key = search.value.trim().toLowerCase()
  if (!key) return []
  return searchableMenus.value.filter((item) => `${item.label} ${item.parent}`.toLowerCase().includes(key)).slice(0, 6)
})
function toggleGroup(label) {
  if (collapsed.value) {
    collapsed.value = false
    openGroups.value[label] = true
    return
  }
  openGroups.value[label] = !openGroups.value[label]
}
watch(() => route.path, (path) => {
  const systemPaths = ['/users', '/roles', '/menus', '/departments', '/positions', '/settings']
  if (systemPaths.includes(path)) openGroups.value['系统管理'] = true
  if (path.startsWith('/logs/')) openGroups.value['日志管理'] = true
  if (['/images', '/attachments', '/videos'].includes(path)) openGroups.value['文件管理'] = true
  if (path.startsWith('/monitor/')) openGroups.value['系统监控'] = true
  if (path.startsWith('/business/')) openGroups.value['业务管理'] = true
}, { immediate: true })
onMounted(async () => {
  loadPublicConfig().catch(() => {})
  try {
    const info = await request.get('/auth/info')
    isAdmin.value = Array.isArray(info.roles) && info.roles.includes('admin')
    profileName.value = info.user?.nickName || info.user?.userName || '用户'
  } catch {
    isAdmin.value = false
  }
})
watch(() => platformConfig.systemName, (name) => {
  document.title = `${route.meta.title || '首页'} - ${name}`
})
function submitSearch() {
  const key = search.value.trim()
  if (!key) return
  const item = groups.value.flatMap((g) => g.children || [g]).find((i) => i.label.includes(key))
  if (item?.path) router.push(item.path)
}
async function logout() {
  try {
    await request.post('/auth/logout')
  } catch {
    // 网络异常时仍清理当前浏览器的登录状态。
  } finally {
    localStorage.removeItem('access_token')
    router.replace('/login')
  }
}
function setTheme(mode) {
  themeMode.value = mode
  localStorage.setItem('theme_mode', mode)
  document.documentElement.dataset.theme = mode
}
function setAccent(color) {
  themeColor.value = applyThemeColor(color)
  customColorInput.value = themeColor.value
  colorError.value = ''
}
function applyCustomColor() {
  const color = normalizeThemeColor(customColorInput.value.trim())
  if (!color) {
    colorError.value = '请输入 6 位十六进制颜色，例如 #176cff'
    return
  }
  setAccent(color)
}
function toggleTabs() {
  showTabs.value = !showTabs.value
  localStorage.setItem('show_tabs', showTabs.value ? '1' : '0')
}
function goSearch(item) {
  if (!item?.path) return
  router.push(item.path)
  search.value = ''
}
</script>

<template>
  <div class="app-shell" :class="{ collapsed, 'has-tabs': showTabs }">
    <header class="topbar">
      <div class="brand" @click="router.push('/')">
        <img v-if="platformConfig.logoUrl" class="brand-logo" :src="platformConfig.logoUrl" alt="系统 Logo" />
        <div v-else class="brand-mark"><i></i><i></i><i></i><i></i></div>
        <div class="brand-copy">
          <strong>{{ platformConfig.systemName }}</strong>
          <span>{{ platformConfig.systemDescription }}</span>
        </div>
      </div>
      <button class="icon-button sidebar-toggle" title="收起菜单" @click="collapsed = !collapsed">
        <PanelLeftClose v-if="!collapsed" :size="21" />
        <Menu v-else :size="21" />
      </button>
      <form class="global-search-wrap" @submit.prevent="submitSearch">
        <div class="global-search">
        <Search :size="17" />
        <input v-model="search" placeholder="搜索菜单、功能、用户、数据..." />
        </div>
        <div v-if="searchResults.length" class="search-results"><button v-for="item in searchResults" :key="item.path" type="button" @mousedown.prevent="goSearch(item)"><component :is="item.icon" :size="16" /><span>{{ item.label }}</span><small>{{ item.parent || '快捷入口' }}</small></button></div>
      </form>
      <div class="top-actions">
        <button class="icon-button" title="全屏" @click="document.documentElement.requestFullscreen?.()"><Maximize2 :size="19" /></button>
        <div class="theme-wrap">
          <button class="icon-button" title="主题设置" :aria-expanded="themeOpen" @click="themeOpen = !themeOpen"><Palette :size="20" /></button>
          <div v-if="themeOpen" class="theme-panel">
            <div class="theme-panel-title">主题设置</div>
            <div class="theme-section-label">主题模式</div>
            <div class="theme-mode-options">
              <button v-for="item in themeOptions" :key="item.value" type="button" :class="{ selected: themeMode === item.value }" @click="setTheme(item.value)">
                <i :style="{ background: item.color }"></i><span>{{ item.name }}</span><Check v-if="themeMode === item.value" :size="14" />
              </button>
            </div>
            <div class="theme-section-label theme-color-heading"><span>主题颜色</span><button type="button" @click="setAccent(DEFAULT_THEME_COLOR)">恢复默认</button></div>
            <div class="accent-presets" role="group" aria-label="预设主题颜色">
              <button v-for="item in THEME_COLOR_PRESETS" :key="item.color" type="button" :title="item.name" :aria-label="item.name" :aria-pressed="themeColor === item.color" :class="{ selected: themeColor === item.color }" :style="{ background: item.color }" @click="setAccent(item.color)">
                <Check v-if="themeColor === item.color" :size="14" />
              </button>
            </div>
            <div class="custom-color-row">
              <label class="custom-color-picker" title="选择自定义颜色">
                <input type="color" :value="themeColor" aria-label="选择自定义主题颜色" @input="setAccent($event.target.value)" />
              </label>
              <input v-model="customColorInput" class="custom-color-hex" maxlength="7" aria-label="十六进制主题颜色" placeholder="#176cff" @keydown.enter.prevent="applyCustomColor" @blur="applyCustomColor" />
              <button type="button" class="custom-color-apply" @click="applyCustomColor">应用</button>
            </div>
            <p v-if="colorError" class="theme-color-error">{{ colorError }}</p>
            <div class="theme-setting"><span>显示页签</span><button class="switch" :class="{ on: showTabs }" @click="toggleTabs"><i></i></button></div>
          </div>
        </div>
        <div class="notice-wrap"><button class="icon-button has-dot" title="消息" @click="notificationsOpen = !notificationsOpen"><Bell :size="20" /></button><div v-if="notificationsOpen" class="notice-panel"><div class="notice-head"><strong>通知消息</strong><button @click="notificationsOpen = false">全部已读</button></div><div class="notice-item"><i class="blue"></i><div><b>系统 V2.0.0 正式发布</b><span>新的权限管理功能已上线</span><time>10分钟前</time></div></div><div class="notice-item"><i class="orange"></i><div><b>系统维护升级通知</b><span>今晚 23:00 进行例行维护</span><time>2小时前</time></div></div><div class="notice-item"><i class="green"></i><div><b>数据导出任务已完成</b><span>数据统计报表.xlsx 可以下载</span><time>昨天</time></div></div><button class="notice-more" @click="notificationsOpen = false">查看全部通知 <ChevronRight :size="14" /></button></div></div>
        <button v-if="isAdmin" class="icon-button" title="系统配置" @click="router.push('/settings')"><Settings :size="20" /></button>
        <div class="profile" @click="showProfile = !showProfile">
          <div class="avatar">{{ profileName.slice(0, 1) }}</div>
          <div class="profile-copy"><strong>{{ profileName }}</strong><span>{{ isAdmin ? '超级管理员' : '普通用户' }}</span></div>
          <ChevronDown :size="15" />
          <div v-if="showProfile" class="profile-menu">
            <button @click.stop="router.push('/profile'); showProfile = false">个人中心</button><button v-if="isAdmin" @click.stop="router.push('/settings'); showProfile = false">系统配置</button><button @click.stop="logout">退出登录</button>
          </div>
        </div>
      </div>
    </header>

    <div v-if="showTabs" class="tabbar"><button :class="{ active: route.path === '/' }" @click="router.push('/')">首页</button><button v-if="route.path !== '/'" class="active">{{ activeLabel }} <span @click.stop="router.push('/')">×</span></button></div>

    <aside class="sidebar">
      <nav>
        <template v-for="group in groups" :key="group.label">
          <button v-if="!group.children" class="nav-item" :class="{ active: route.path === group.path }" @click="router.push(group.path)">
            <component :is="group.icon" :size="20" /><span>{{ group.label }}</span>
            <ChevronRight v-if="['/business','/files'].includes(group.path)" class="nav-chevron" :size="15" />
          </button>
          <div v-else class="nav-group">
            <button class="nav-title" :aria-expanded="openGroups[group.label]" @click="toggleGroup(group.label)"><component :is="group.icon" :size="20" /><span>{{ group.label }}</span><ChevronDown class="nav-chevron group-arrow" :class="{ rotated: openGroups[group.label] }" :size="15" /></button>
            <div class="sub-nav" :class="{ expanded: openGroups[group.label] }">
              <div class="sub-nav-inner" :inert="!openGroups[group.label]">
                <button v-for="child in group.children" :key="child.path" class="nav-item child" :class="{ active: route.path === child.path }" @click="router.push(child.path)">
                  <component :is="child.icon" :size="19" /><span>{{ child.label }}</span>
                </button>
              </div>
            </div>
          </div>
        </template>
      </nav>
    </aside>

    <main class="main-area">
      <div v-if="route.path !== '/'" class="breadcrumb"><span>首页</span><ChevronRight :size="14" /><strong>{{ activeLabel }}</strong></div>
      <router-view v-slot="{ Component }">
        <transition name="page" mode="out-in"><component :is="Component" /></transition>
      </router-view>
      <footer><span>© 2024 {{ platformConfig.systemName }}　v2.0.0</span><span>帮助文档　 |　 关于我们　 |　 隐私政策</span></footer>
    </main>
  </div>
</template>
