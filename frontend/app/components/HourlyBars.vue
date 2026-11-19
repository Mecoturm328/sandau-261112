<script setup lang="ts">
import { computed } from 'vue'
import type { HourlyPoint } from '~/types/dashboard'
import { formatNumber } from '~/utils/format'

const props = defineProps<{
  hourly: HourlyPoint[]
}>()

const totalOrders = computed(() => props.hourly.reduce((sum, item) => sum + item.orders, 0))
const maxOrders = computed(() => Math.max(0, ...props.hourly.map((item) => item.orders)))
const peakIndex = computed(() => props.hourly.findIndex((item) => item.orders === maxOrders.value))
const peak = computed(() => props.hourly[peakIndex.value] ?? null)

const bars = computed(() => props.hourly.map((item, index) => ({
  ...item,
  isPeak: index === peakIndex.value,
  height: maxOrders.value ? Math.max(2, (item.orders / maxOrders.value) * 100) : 0,
})))

const ariaLabel = computed(() => (
  peak.value
    ? `按小时统计订单量，高峰时段为 ${peak.value.label}，共 ${formatNumber(peak.value.orders)} 单。`
    : '所选时段没有分时订单数据'
))
</script>

<template>
  <div class="hourly-chart" role="img" :aria-label="ariaLabel">
    <template v-if="totalOrders > 0">
      <div class="hourly-bars">
        <div
          v-for="bar in bars"
          :key="bar.hour"
          class="hour-column"
          :class="{ 'is-peak': bar.isPeak }"
          :title="`${bar.label} · ${formatNumber(bar.orders)} 单`"
        >
          <span class="hour-value">{{ formatNumber(bar.orders) }}</span>
          <div class="hour-track">
            <div class="hour-bar" :style="{ '--bar-height': `${bar.height.toFixed(2)}%` }" />
          </div>
        </div>
      </div>
      <div class="hour-labels" aria-hidden="true">
        <span v-for="bar in bars" :key="bar.hour" class="hour-label">{{ bar.label.slice(0, 2) }}</span>
      </div>
    </template>
    <div v-else class="empty-chart">
      <span class="empty-mark" aria-hidden="true">∅</span>
      <p>这个时段还没有订单</p>
    </div>
  </div>
</template>
