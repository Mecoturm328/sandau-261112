<script setup lang="ts">
import { computed } from 'vue'
import type { CategorySlice } from '~/types/dashboard'
import { formatCompactMoney, formatDecimal, formatMoney } from '~/utils/format'

const props = defineProps<{
  categories: CategorySlice[]
}>()

const COLORS: Record<string, string> = {
  咖啡: '#cf6d3f',
  烘焙: '#c7a25e',
  非咖啡: '#8ba58e',
  轻食: '#718a93',
}
const FALLBACK_COLORS = ['#cf6d3f', '#c7a25e', '#8ba58e', '#718a93', '#a17a67']
const RADIUS = 61
const CIRCUMFERENCE = 2 * Math.PI * RADIUS

const total = computed(() => props.categories.reduce((sum, item) => sum + item.revenue, 0))

const segments = computed(() => {
  let offset = 0
  return props.categories.map((category, index) => {
    const length = CIRCUMFERENCE * (category.share / 100)
    const visible = Math.max(0.8, length - 2.2)
    const segment = {
      ...category,
      color: COLORS[category.name] || FALLBACK_COLORS[index % FALLBACK_COLORS.length]!,
      visible,
      remaining: CIRCUMFERENCE - visible,
      offset,
      dashOffset: -offset,
    }
    offset += length
    return segment
  })
})

const ariaLabel = computed(() => (
  props.categories.length
    ? `品类营业额占比：${props.categories.map((item) => `${item.name} ${formatDecimal(item.share)}%`).join('，')}`
    : '所选时段没有品类数据'
))
</script>

<template>
  <div class="category-body">
    <div class="donut-stage" role="img" :aria-label="ariaLabel">
      <svg v-if="total > 0" class="donut-svg" viewBox="0 0 160 160" aria-hidden="true">
        <circle class="donut-track" cx="80" cy="80" :r="RADIUS" />
        <circle
          v-for="segment in segments"
          :key="segment.name"
          class="donut-segment"
          cx="80"
          cy="80"
          :r="RADIUS"
          :stroke="segment.color"
          :stroke-dasharray="`${segment.visible.toFixed(2)} ${segment.remaining.toFixed(2)}`"
          :stroke-dashoffset="segment.dashOffset.toFixed(2)"
          transform="rotate(-90 80 80)"
        >
          <title>{{ segment.name }} · {{ formatDecimal(segment.share) }}% · {{ formatMoney(segment.revenue) }}</title>
        </circle>
        <text class="donut-total" x="80" y="78">{{ formatCompactMoney(total) }}</text>
        <text class="donut-caption" x="80" y="93">品类营收</text>
      </svg>
      <svg v-else class="donut-svg" viewBox="0 0 160 160" aria-hidden="true">
        <circle class="donut-track" cx="80" cy="80" :r="RADIUS" />
        <text class="donut-total" x="80" y="78">∅</text>
        <text class="donut-caption" x="80" y="94">暂无品类数据</text>
      </svg>
    </div>

    <ul class="category-legend">
      <template v-if="segments.length">
        <li v-for="segment in segments" :key="segment.name" class="category-item">
          <span class="category-swatch" :style="{ background: segment.color }" aria-hidden="true" />
          <span class="category-label">
            <strong>{{ segment.name }}</strong>
            <small>{{ formatMoney(segment.revenue) }}</small>
          </span>
          <span class="category-share">{{ formatDecimal(segment.share) }}%</span>
        </li>
      </template>
      <li v-else class="category-item">
        <span class="category-label"><strong>换个日期试试</strong></span>
      </li>
    </ul>
  </div>
</template>
