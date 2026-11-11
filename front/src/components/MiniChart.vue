<script setup>
defineProps({ color: { type: String, default: '#1677ff' }, points: { type: String, default: '2,34 18,22 30,26 45,10 58,14 72,2' } })
</script>
<template>
  <svg viewBox="0 0 74 38" class="mini-chart">
    <defs><linearGradient :id="`grad-${color.replace('#','')}`" x1="0" y1="0" x2="0" y2="1"><stop :stop-color="color" stop-opacity=".25"/><stop offset="1" :stop-color="color" stop-opacity="0"/></linearGradient></defs>
    <polygon :points="`2,38 ${points} 72,38`" :fill="`url(#grad-${color.replace('#','')})`"/>
    <polyline :points="points" fill="none" :stroke="color" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
  </svg>
</template>
