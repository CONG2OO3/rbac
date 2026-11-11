<script setup>
import { onMounted, onUnmounted, ref, watch } from 'vue'
import * as echarts from 'echarts/core'
import { LineChart } from 'echarts/charts'
import { GridComponent, LegendComponent, TooltipComponent } from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'

echarts.use([LineChart, GridComponent, LegendComponent, TooltipComponent, CanvasRenderer])

const props = defineProps({
  points: { type: Array, default: () => [] },
})

const chartElement = ref(null)
let chart
let resizeObserver
let themeObserver

function render() {
  if (!chart) return
  const dark = document.documentElement.dataset.theme === 'dark'
  const accent = getComputedStyle(document.documentElement).getPropertyValue('--blue').trim() || '#176cff'
  const red = Number.parseInt(accent.slice(1, 3), 16)
  const green = Number.parseInt(accent.slice(3, 5), 16)
  const blue = Number.parseInt(accent.slice(5, 7), 16)
  const textColor = dark ? '#aab8cc' : '#7b8faa'
  const gridColor = dark ? '#303039' : '#e9eff7'
  chart.setOption({
    animationDuration: 350,
    color: [accent, '#58b795'],
    tooltip: { trigger: 'axis' },
    legend: { bottom: 0, itemWidth: 9, itemHeight: 9, textStyle: { color: textColor, fontSize: 11 } },
    grid: { left: 42, right: 22, top: 13, bottom: 43 },
    xAxis: {
      type: 'category',
      boundaryGap: false,
      data: props.points.map(item => item.label),
      axisLine: { lineStyle: { color: gridColor } },
      axisTick: { show: false },
      axisLabel: { color: textColor, fontSize: 11 },
    },
    yAxis: {
      type: 'value',
      min: 0,
      minInterval: 1,
      axisLabel: { color: textColor, fontSize: 11 },
      splitLine: { lineStyle: { color: gridColor } },
    },
    series: [
      {
        name: 'PV 访问次数', type: 'line', smooth: true, symbolSize: 6,
        showSymbol: props.points.length <= 12,
        data: props.points.map(item => item.pv),
        lineStyle: { width: 3 },
        areaStyle: { color: `rgba(${red}, ${green}, ${blue}, .1)` },
      },
      {
        name: 'UV 独立用户', type: 'line', smooth: true, symbolSize: 6,
        showSymbol: props.points.length <= 12,
        data: props.points.map(item => item.uv),
        lineStyle: { width: 3 },
      },
    ],
  })
}

watch(() => props.points, render)
onMounted(() => {
  chart = echarts.init(chartElement.value)
  resizeObserver = new ResizeObserver(() => chart.resize())
  resizeObserver.observe(chartElement.value)
  themeObserver = new MutationObserver(render)
  themeObserver.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme', 'style'] })
  render()
})
onUnmounted(() => {
  resizeObserver?.disconnect()
  themeObserver?.disconnect()
  chart?.dispose()
})
</script>

<template>
  <div ref="chartElement" class="traffic-chart" role="img" aria-label="PV 和 UV 访问趋势图"></div>
</template>

<style scoped>
.traffic-chart { width: 100%; height: 170px; }
</style>
