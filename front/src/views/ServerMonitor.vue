<script setup>
import { nextTick, onMounted, onUnmounted, ref } from 'vue'
import * as echarts from 'echarts/core'
import { GaugeChart, LineChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import { Activity, Cpu, Database, HardDrive, MemoryStick, RefreshCw, Server, Wifi } from 'lucide-vue-next'
import { getServerSnapshot } from '../api/monitor'

echarts.use([GaugeChart, LineChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

const snapshot = ref(null)
const loading = ref(false)
const error = ref('')
const cpuElement = ref(null)
const memoryElement = ref(null)
const diskElement = ref(null)
const charts = []
const history = ref([])
let pollTimer
let resizeObserver
let themeObserver
let pending = false

function formatBytes(value) {
  if (!value) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB', 'TB']
  let result = value
  let index = 0
  while (result >= 1024 && index < units.length - 1) {
    result /= 1024
    index++
  }
  return `${result.toFixed(index === 0 ? 0 : 1)} ${units[index]}`
}

function formatUptime(value) {
  const minutes = Math.floor((value || 0) / 60000)
  const days = Math.floor(minutes / 1440)
  const hours = Math.floor((minutes % 1440) / 60)
  return days ? `${days} 天 ${hours} 小时` : `${hours} 小时 ${minutes % 60} 分钟`
}

function ratio(used, total) {
  return total > 0 ? Math.round(used / total * 1000) / 10 : 0
}

function chartColors() {
  const dark = document.documentElement.dataset.theme === 'dark'
  const accent = getComputedStyle(document.documentElement).getPropertyValue('--blue').trim() || '#176cff'
  const rgb = [1, 3, 5].map(index => Number.parseInt(accent.slice(index, index + 2), 16))
  return {
    text: dark ? '#aebbd0' : '#7185a3',
    grid: dark ? '#303039' : '#eaf0f7',
    accent,
    accentArea: `rgba(${rgb.join(', ')}, .12)`,
  }
}

function renderCharts() {
  if (!snapshot.value || charts.length !== 3) return
  const current = snapshot.value
  const colors = chartColors()
  const times = history.value.map(point => point.time)
  const baseLine = {
    animationDuration: 350,
    tooltip: { trigger: 'axis' },
    grid: { left: 38, right: 18, top: 22, bottom: 28 },
    xAxis: { type: 'category', boundaryGap: false, data: times, axisLabel: { color: colors.text }, axisLine: { lineStyle: { color: colors.grid } }, axisTick: { show: false } },
    yAxis: { type: 'value', min: 0, max: 100, axisLabel: { formatter: '{value}%', color: colors.text }, splitLine: { lineStyle: { color: colors.grid } } },
  }
  charts[0].setOption({
    ...baseLine,
    series: [{ type: 'line', name: 'CPU 使用率', smooth: true, showSymbol: history.value.length === 1,
      data: history.value.map(point => point.cpu), lineStyle: { width: 3, color: colors.accent }, itemStyle: { color: colors.accent },
      areaStyle: { color: colors.accentArea } }],
  })
  charts[1].setOption({
    ...baseLine,
    legend: { data: ['物理内存', 'JVM 堆内存'], top: 0, textStyle: { color: colors.text } },
    grid: { ...baseLine.grid, top: 38 },
    series: [
      { type: 'line', name: '物理内存', smooth: true, showSymbol: false, data: history.value.map(point => point.memory), lineStyle: { width: 3, color: '#10b981' }, itemStyle: { color: '#10b981' } },
      { type: 'line', name: 'JVM 堆内存', smooth: true, showSymbol: false, data: history.value.map(point => point.heap), lineStyle: { width: 3, color: '#8559ed' }, itemStyle: { color: '#8559ed' } },
    ],
  })
  charts[2].setOption({
    series: [{ type: 'gauge', min: 0, max: 100, radius: '88%', center: ['50%', '54%'],
      progress: { show: true, width: 16, itemStyle: { color: '#f59e37' } },
      axisLine: { lineStyle: { width: 16, color: [[1, colors.grid]] } }, axisTick: { show: false }, splitLine: { show: false }, axisLabel: { show: false }, pointer: { show: false },
      detail: { formatter: '{value}%', color: colors.text, fontSize: 28, fontWeight: 700, offsetCenter: [0, '10%'] },
      title: { show: false }, data: [{ value: ratio(current.diskUsed, current.diskTotal) }],
    }],
  })
}

async function loadSnapshot() {
  if (pending) return
  pending = true
  loading.value = !snapshot.value
  try {
    const result = await getServerSnapshot()
    snapshot.value = result
    error.value = ''
    history.value = [...history.value, {
      time: new Date(result.sampledAt).toLocaleTimeString('zh-CN', { hour12: false }),
      cpu: result.cpuPercent,
      memory: ratio(result.physicalMemoryUsed, result.physicalMemoryTotal),
      heap: ratio(result.heapUsed, result.heapMax),
    }].slice(-30)
    await nextTick()
    renderCharts()
  } catch (exception) {
    error.value = exception.message || '服务监控数据加载失败'
  } finally {
    loading.value = false
    pending = false
  }
}

onMounted(async () => {
  charts.push(echarts.init(cpuElement.value), echarts.init(memoryElement.value), echarts.init(diskElement.value))
  resizeObserver = new ResizeObserver(() => charts.forEach(chart => chart.resize()))
  ;[cpuElement.value, memoryElement.value, diskElement.value].forEach(element => resizeObserver.observe(element))
  themeObserver = new MutationObserver(renderCharts)
  themeObserver.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme', 'style'] })
  await loadSnapshot()
  pollTimer = window.setInterval(loadSnapshot, 5000)
})

onUnmounted(() => {
  window.clearInterval(pollTimer)
  resizeObserver?.disconnect()
  themeObserver?.disconnect()
  charts.forEach(chart => chart.dispose())
})
</script>

<template>
  <div class="monitor-page">
    <div class="page-intro">
      <div><h1>服务监控</h1><p>实时查看服务器资源、JVM 状态和基础服务运行情况</p></div>
      <button class="monitor-refresh" :disabled="loading" @click="loadSnapshot"><RefreshCw :size="16" />刷新数据</button>
    </div>
    <div v-if="error" class="api-error">{{ error }}</div>
    <div class="monitor-meta"><span class="live-dot"></span>每 5 秒更新一次<span v-if="snapshot"> · 最近采样 {{ snapshot.sampledAt.replace('T', ' ') }}</span></div>

    <div class="monitor-cards">
      <section class="panel monitor-card"><div class="monitor-card-icon blue"><Cpu :size="22" /></div><div><span>CPU 使用率</span><strong>{{ snapshot ? `${snapshot.cpuPercent}%` : '--' }}</strong><small>{{ snapshot?.processorCount ?? '--' }} 核处理器</small></div></section>
      <section class="panel monitor-card"><div class="monitor-card-icon green"><MemoryStick :size="22" /></div><div><span>物理内存</span><strong>{{ snapshot ? `${ratio(snapshot.physicalMemoryUsed, snapshot.physicalMemoryTotal)}%` : '--' }}</strong><small>{{ snapshot ? `${formatBytes(snapshot.physicalMemoryUsed)} / ${formatBytes(snapshot.physicalMemoryTotal)}` : '正在读取' }}</small></div></section>
      <section class="panel monitor-card"><div class="monitor-card-icon purple"><Activity :size="22" /></div><div><span>JVM 堆内存</span><strong>{{ snapshot ? `${ratio(snapshot.heapUsed, snapshot.heapMax)}%` : '--' }}</strong><small>{{ snapshot ? `${formatBytes(snapshot.heapUsed)} / ${formatBytes(snapshot.heapMax)}` : '正在读取' }}</small></div></section>
      <section class="panel monitor-card"><div class="monitor-card-icon orange"><HardDrive :size="22" /></div><div><span>磁盘使用率</span><strong>{{ snapshot ? `${ratio(snapshot.diskUsed, snapshot.diskTotal)}%` : '--' }}</strong><small>{{ snapshot ? `${formatBytes(snapshot.diskUsed)} / ${formatBytes(snapshot.diskTotal)}` : '正在读取' }}</small></div></section>
    </div>

    <div class="monitor-charts">
      <section class="panel monitor-chart"><div class="monitor-section-title"><h2>CPU 使用趋势</h2><span>最近 30 次采样</span></div><div ref="cpuElement" class="chart-canvas"></div></section>
      <section class="panel monitor-chart"><div class="monitor-section-title"><h2>内存使用趋势</h2><span>最近 30 次采样</span></div><div ref="memoryElement" class="chart-canvas"></div></section>
    </div>

    <div class="monitor-bottom">
      <section class="panel disk-card"><div class="monitor-section-title"><h2>磁盘容量</h2><span>当前工作盘</span></div><div ref="diskElement" class="disk-canvas"></div><p>{{ snapshot ? `${formatBytes(snapshot.diskUsed)} 已使用，剩余 ${formatBytes(snapshot.diskTotal - snapshot.diskUsed)}` : '正在读取磁盘容量' }}</p></section>
      <section class="panel server-info"><div class="monitor-section-title"><h2>运行信息</h2></div><div class="info-grid"><div><span>操作系统</span><strong>{{ snapshot?.osName || '--' }}</strong></div><div><span>Java 版本</span><strong>{{ snapshot?.javaVersion || '--' }}</strong></div><div><span>运行时长</span><strong>{{ snapshot ? formatUptime(snapshot.uptimeMillis) : '--' }}</strong></div><div><span>当前线程</span><strong>{{ snapshot?.threadCount ?? '--' }}</strong></div></div><div class="service-status"><div><Database :size="18" /><span>MySQL</span><b :class="snapshot?.mysqlHealthy ? 'healthy' : 'unhealthy'">{{ snapshot ? (snapshot.mysqlHealthy ? '运行正常' : '连接异常') : '检测中' }}</b></div><div><Wifi :size="18" /><span>Redis</span><b :class="snapshot?.redisHealthy ? 'healthy' : 'unhealthy'">{{ snapshot ? (snapshot.redisHealthy ? '运行正常' : '连接异常') : '检测中' }}</b></div><div><Server :size="18" /><span>应用服务</span><b class="healthy">{{ snapshot ? '运行正常' : '检测中' }}</b></div></div></section>
    </div>
  </div>
</template>

<style scoped>
.monitor-page{min-width:0}.monitor-refresh{height:36px;display:flex;align-items:center;gap:7px;padding:0 13px;border:1px solid #dae5f4;border-radius:6px;color:#587397;background:#fff;font-size:13px}.monitor-refresh:hover{color:#176cff;border-color:#abc8f8}.monitor-refresh:disabled{opacity:.55}.monitor-meta{display:flex;align-items:center;gap:7px;margin:-6px 0 14px;color:#8b9db5;font-size:12px}.live-dot{width:7px;height:7px;border-radius:50%;background:#10b981;box-shadow:0 0 0 3px #10b9811f}.monitor-cards{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:14px}.monitor-card{height:118px;display:flex;align-items:center;gap:16px;padding:18px}.monitor-card-icon{width:52px;height:52px;flex:none;display:grid;place-items:center;border-radius:14px}.monitor-card-icon.blue{color:#176cff;background:#eaf2ff}.monitor-card-icon.green{color:#0ba876;background:#e7f8f0}.monitor-card-icon.purple{color:#8150e8;background:#f0e9ff}.monitor-card-icon.orange{color:#f49422;background:#fff3e4}.monitor-card>div:last-child{min-width:0;display:flex;flex-direction:column}.monitor-card span{color:#8193ad;font-size:12px}.monitor-card strong{margin:6px 0 4px;color:#183052;font-size:24px;line-height:1}.monitor-card small{overflow:hidden;color:#9aa9bc;font-size:11px;text-overflow:ellipsis;white-space:nowrap}.monitor-charts{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:14px;margin-top:14px}.monitor-chart{min-width:0;padding:17px 18px 10px}.monitor-section-title{display:flex;align-items:center;justify-content:space-between}.monitor-section-title h2{margin:0;color:#233d60;font-size:15px}.monitor-section-title span{color:#9aa9bc;font-size:11px}.chart-canvas{width:100%;height:265px}.monitor-bottom{display:grid;grid-template-columns:minmax(310px,.85fr) minmax(0,1.5fr);gap:14px;margin-top:14px}.disk-card,.server-info{padding:18px}.disk-canvas{height:216px;width:100%}.disk-card p{margin:0;color:#8b9db5;text-align:center;font-size:12px}.info-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:0 22px;margin-top:12px}.info-grid>div{min-height:48px;display:flex;align-items:center;justify-content:space-between;gap:8px;border-bottom:1px solid #edf1f7;font-size:12px}.info-grid span{color:#8c9db4}.info-grid strong{overflow:hidden;color:#405a7a;font-weight:500;text-overflow:ellipsis;white-space:nowrap}.service-status{display:grid;grid-template-columns:repeat(3,1fr);gap:11px;margin-top:19px}.service-status>div{min-width:0;display:flex;align-items:center;gap:8px;padding:13px;border:1px solid #edf1f7;border-radius:7px;color:#7188a4;font-size:12px}.service-status b{margin-left:auto;font-size:11px;font-weight:500;white-space:nowrap}.healthy{color:#0ca876}.unhealthy{color:#e65a5a}:global(:root[data-theme="dark"]) .monitor-refresh{border-color:#383840;color:#bdc8d8;background:#1b1b1f}:global(:root[data-theme="dark"]) .monitor-card strong,:global(:root[data-theme="dark"]) .monitor-section-title h2{color:#e5ebf5}:global(:root[data-theme="dark"]) .info-grid>div,:global(:root[data-theme="dark"]) .service-status>div{border-color:#303039}:global(:root[data-theme="dark"]) .info-grid strong{color:#d1dbe8}@media(max-width:1350px){.monitor-cards{grid-template-columns:repeat(2,minmax(0,1fr))}.monitor-bottom{grid-template-columns:1fr}}
</style>
