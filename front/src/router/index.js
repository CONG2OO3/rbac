import { createRouter, createWebHistory } from 'vue-router'
import Dashboard from '../views/Dashboard.vue'
import ResourcePage from '../views/ResourcePage.vue'
import SystemSettings from '../views/SystemSettings.vue'
import Profile from '../views/Profile.vue'
import Login from '../views/Login.vue'
import LogManagement from '../views/LogManagement.vue'
import FileManagement from '../views/FileManagement.vue'
import { recordVisit } from '../api/traffic'
import ServerMonitor from '../views/ServerMonitor.vue'
import OnlineUsers from '../views/OnlineUsers.vue'
import ProductManagement from '../views/ProductManagement.vue'
import OrderManagement from '../views/OrderManagement.vue'
import Register from '../views/Register.vue'
import { platformConfig } from '../api/platformConfig'

const resources = [
  ['users', '用户管理', '维护企业成员、账号状态与组织归属'],
  ['roles', '角色管理', '配置角色权限和数据范围'],
  ['menus', '菜单管理', '维护系统导航、按钮及接口权限'],
  ['departments', '部门管理', '管理组织架构及部门负责人'],
  ['positions', '岗位管理', '维护岗位职级与人员编制'],
  ['data', '数据管理', '查看数据资产、来源及使用情况'],
]

const routes = [
  { path: '/login', name: 'login', component: Login, meta: { title: '登录', public: true } },
  { path: '/register', name: 'register', component: Register, meta: { title: '注册', public: true } },
  { path: '/', name: 'dashboard', component: Dashboard, meta: { title: '首页' } },
  { path: '/settings', name: 'settings', component: SystemSettings, meta: { title: '系统设置' } },
  { path: '/profile', name: 'profile', component: Profile, meta: { title: '个人中心' } },
  { path: '/logs', redirect: '/logs/login' },
  { path: '/logs/:type(login|operation)', name: 'logs', component: LogManagement, meta: { title: '日志管理' } },
  { path: '/monitor', redirect: '/monitor/server' },
  { path: '/monitor/server', name: 'server-monitor', component: ServerMonitor, meta: { title: '服务监控' } },
  { path: '/monitor/online', name: 'online-users', component: OnlineUsers, meta: { title: '在线用户' } },
  { path: '/business', redirect: '/business/products' },
  { path: '/business/products', name: 'products', component: ProductManagement, meta: { title: '产品管理' } },
  { path: '/business/orders', name: 'orders', component: OrderManagement, meta: { title: '订单管理' } },
  { path: '/files', redirect: '/images' },
  {
    path: '/images',
    name: 'images',
    component: FileManagement,
    props: {
      fileType: 'image',
      title: '图片管理',
      subtitle: '集中管理企业图片素材、预览资源和存储空间',
    },
    meta: { title: '图片管理' },
  },
  {
    path: '/attachments',
    name: 'attachments',
    component: FileManagement,
    props: {
      fileType: 'attachment',
      title: '附件管理',
      subtitle: '管理业务附件、文档和其他文件资源',
    },
    meta: { title: '附件管理' },
  },
  {
    path: '/videos',
    name: 'videos',
    component: FileManagement,
    props: {
      fileType: 'video',
      title: '视频管理',
      subtitle: '集中管理企业视频素材，支持在线播放、下载和存储管理',
    },
    meta: { title: '视频管理' },
  },
  ...resources.map(([path, title, subtitle]) => ({
    path: `/${path}`,
    name: path,
    component: ResourcePage,
    props: { resource: path, title, subtitle },
    meta: { title },
  })),
]

const router = createRouter({ history: createWebHistory(), routes })
router.beforeEach((to) => {
  const token = localStorage.getItem('access_token')
  if (to.meta.public && token && to.name === 'login') return { path: '/' }
  if (!to.meta.public && !token) return { path: '/login', query: { redirect: to.fullPath } }
  return true
})
router.afterEach((to) => {
  document.title = `${to.meta.title || '首页'} - ${platformConfig.systemName}`
  if (!to.meta.public && to.path !== '/' && localStorage.getItem('access_token')) {
    recordVisit().catch(() => {})
  }
})

export default router
