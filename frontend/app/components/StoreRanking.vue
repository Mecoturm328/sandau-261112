<script setup lang="ts">
import { computed } from 'vue'
import type { StorePerformance } from '~/types/dashboard'
import { formatMoney, formatNumber, safeColor } from '~/utils/format'

const props = defineProps<{
  stores: StorePerformance[]
}>()

const totalRevenue = computed(() => props.stores.reduce((sum, store) => sum + store.revenue, 0))
const maxRevenue = computed(() => Math.max(0, ...props.stores.map((store) => store.revenue)))

function barWidth(revenue: number): number {
  return maxRevenue.value ? Math.max(1.5, (revenue / maxRevenue.value) * 100) : 0
}
</script>

<template>
  <div class="store-ranking" :class="{ 'is-single': stores.length === 1 }">
    <div v-if="!stores.length || totalRevenue === 0" class="store-empty">
      这个时段还没有门店营业数据
    </div>
    <div v-for="(store, index) in stores" v-else :key="store.id" class="store-row">
      <span class="store-rank" aria-hidden="true">{{ String(index + 1).padStart(2, '0') }}</span>
      <div class="store-main">
        <div class="store-line">
          <strong>{{ store.name }}</strong>
          <span class="store-address">{{ store.address }}</span>
        </div>
        <div class="store-track" aria-hidden="true">
          <span :style="{ width: `${barWidth(store.revenue).toFixed(2)}%`, background: safeColor(store.accent) }" />
        </div>
      </div>
      <div class="store-numbers">
        <strong>{{ formatMoney(store.revenue) }}</strong>
        <span>{{ formatNumber(store.orders) }} 单 · 客单 {{ formatMoney(store.avgTicket) }}</span>
      </div>
    </div>
  </div>
</template>
