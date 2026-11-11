<script setup>
import { onMounted, ref, watch } from 'vue'
import { ArrowRight, ArrowUpRight, UserRound, UsersRound, Layers3, FileText, Server, Database, HardDrive, Box, TriangleAlert, ClipboardList, ChevronRight } from 'lucide-vue-next'
import HeroIllustration from '../components/HeroIllustration.vue'
import MiniChart from '../components/MiniChart.vue'
import DashboardTrafficChart from '../components/DashboardTrafficChart.vue'
import { getTraffic, recordVisit } from '../api/traffic'
import { platformConfig } from '../api/platformConfig'

const range = ref('近7天')
const traffic = ref({ todayPv: 0, todayUv: 0, totalPv: 0, totalUv: 0, points: [] })
const trafficLoading = ref(true)
const trafficError = ref('')
let trafficRequestId = 0
const rangeValues = { '近7天': '7d', '近30天': '30d', '近一年': '1y' }

function formatCount(value) {
  return Number(value || 0).toLocaleString('zh-CN')
}

async function loadTraffic() {
  const requestId = ++trafficRequestId
  trafficLoading.value = true
  trafficError.value = ''
  try {
    const result = await getTraffic(rangeValues[range.value])
    if (requestId === trafficRequestId) traffic.value = result
  } catch (exception) {
    if (requestId === trafficRequestId) trafficError.value = exception.message || '访问数据加载失败'
  } finally {
    if (requestId === trafficRequestId) trafficLoading.value = false
  }
}

watch(range, loadTraffic)
onMounted(async () => {
  try {
    await recordVisit()
  } catch {
    // 访问记录失败时仍加载已有统计。
  }
  await loadTraffic()
})
const cards = [
  { label: '用户总数', value: '1,286', change: '+12.5%', icon: UserRound, tone: 'blue', points: '2,34 18,24 30,27 45,14 58,18 72,4' },
  { label: '部门数量', value: '36', change: '+9.1%', icon: UsersRound, tone: 'green', points: '2,32 18,22 30,26 45,12 58,16 72,3' },
  { label: '数据总量', value: '125,680', change: '+18.3%', icon: Layers3, tone: 'purple', points: '2,31 18,19 30,25 45,11 58,16 72,3' },
  { label: '文件数量', value: '8,932', change: '+6.7%', icon: FileText, tone: 'orange', points: '2,32 18,23 30,28 45,11 58,18 72,5' },
]
const announcements = [
  ['系统 V2.0.0 正式发布', '2024-09-25', 'blue'], ['关于中秋节放假安排的通知', '2024-09-20', 'blue'], ['系统维护升级通知', '2024-09-18', 'orange'], ['新功能：数据导出优化', '2024-09-15', 'blue'], ['安全提示：请及时修改密码', '2024-09-10', 'blue'],
]
const activities = [['2024-09-26 14:32:18','张三','用户管理','新增用户：lisi'],['2024-09-26 11:20:05','李四','角色管理','更新角色：普通用户'],['2024-09-26 10:15:33','王五','文件管理','上传文件：产品需求文档.pdf'],['2024-09-26 09:42:11','赵六','系统设置','修改系统配置']]
</script>

<template>
  <div class="dashboard">
    <section class="welcome-card">
      <div class="welcome-copy"><p>上午好，张三！</p><h1>欢迎使用<span>{{ platformConfig.systemName }}</span></h1><div class="welcome-sub">{{ platformConfig.systemDescription }}</div><div class="welcome-actions"><button class="primary-btn" @click="$router.push('/users')">快速开始 <ArrowRight :size="17" /></button><button class="ghost-btn">使用文档</button></div></div>
      <HeroIllustration /><div class="welcome-slogan"><b>统一管理</b><b>高效协同</b><b>安全稳定</b><i></i></div>
    </section>
    <section class="metric-grid"><article v-for="card in cards" :key="card.label" class="metric-card"><div class="metric-icon" :class="card.tone"><component :is="card.icon" :size="25" /></div><div class="metric-copy"><span>{{ card.label }}</span><strong>{{ card.value }}</strong><small>较上月 <em :class="card.tone">{{ card.change }}</em> <ArrowUpRight :size="14" /></small></div><MiniChart :color="card.tone === 'green' ? '#13b981' : card.tone === 'purple' ? '#7c3aed' : card.tone === 'orange' ? '#ff8a00' : '#1677ff'" :points="card.points" /></article></section>
    <section class="dashboard-grid">
      <article class="panel data-overview traffic-panel">
        <div class="panel-head"><h2>访问趋势</h2><div class="range-tabs"><button v-for="item in ['近7天','近30天','近一年']" :key="item" :class="{ selected: range === item }" @click="range = item">{{ item }}</button></div></div>
        <div class="traffic-summary">
          <div><span>今日 PV</span><strong>{{ formatCount(traffic.todayPv) }}</strong><small>访问次数</small></div>
          <div><span>今日 UV</span><strong>{{ formatCount(traffic.todayUv) }}</strong><small>独立访问用户</small></div>
          <p v-if="trafficError" class="traffic-error">{{ trafficError }}</p>
          <p v-else-if="trafficLoading" class="traffic-loading">正在更新趋势...</p>
        </div>
        <DashboardTrafficChart :points="traffic.points" />
      </article>
      <article class="panel system-status"><div class="panel-head"><h2>系统状态</h2><a>查看更多 <ChevronRight :size="14" /></a></div><div class="status-list"><div><Server :size="19"/><span>服务器状态</span><b><i></i>运行中</b></div><div><Database :size="19"/><span>数据库状态</span><b><i></i>运行中</b></div><div><Box :size="19"/><span>存储状态</span><b><i></i>运行中</b></div><div><TriangleAlert :size="19"/><span>系统负载</span><em>32%</em><div class="progress"><i style="width:32%"></i></div></div><div><HardDrive :size="19"/><span>内存使用</span><em>48%</em><div class="progress"><i style="width:48%"></i></div></div><div><HardDrive :size="19"/><span>磁盘使用</span><em>62%</em><div class="progress"><i style="width:62%"></i></div></div></div></article>
      <article class="panel announcements"><div class="panel-head"><h2>系统公告</h2><a>查看更多 <ChevronRight :size="14" /></a></div><ul><li v-for="item in announcements" :key="item[0]"><i :class="item[2]"></i><span>{{ item[0] }}</span><time>{{ item[1] }}</time></li></ul></article>
    </section>
    <section class="panel activity-panel"><div class="panel-head"><h2>最近操作</h2><a>查看更多 <ChevronRight :size="14" /></a></div><div class="activity-table"><div class="table-row table-head"><span>操作时间</span><span>操作用户</span><span>操作模块</span><span>操作内容</span><span>操作结果</span></div><div v-for="row in activities" :key="row[0]" class="table-row"><span>{{ row[0] }}</span><span>{{ row[1] }}</span><span>{{ row[2] }}</span><span>{{ row[3] }}</span><span class="success"><i></i>成功</span></div></div></section>
  </div>
</template>

<style scoped>
.traffic-panel{overflow:hidden}.traffic-summary{height:49px;display:flex;align-items:center;gap:22px;padding:5px 18px 0}.traffic-summary>div{display:flex;align-items:baseline;gap:6px}.traffic-summary span{color:#7890ac;font-size:11px}.traffic-summary strong{color:#216ee4;font-size:20px;line-height:1;font-weight:700}.traffic-summary>div:nth-child(2) strong{color:#22a77c}.traffic-summary small{color:#9baabc;font-size:10px}.traffic-summary p{margin-left:auto;font-size:11px}.traffic-error{color:#d9485f}.traffic-loading{color:#95a7bf}:global(:root[data-theme="dark"]) .traffic-summary span{color:#9aabc0}
</style>
