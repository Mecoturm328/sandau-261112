<script setup lang="ts">
import { computed } from 'vue'
import type { Store } from '~/types/dashboard'
import { formatCompactMoney, formatNumber, safeColor } from '~/utils/format'

const props = defineProps<{
  stores: Store[]
  totalOrders: number
  totalRevenue: number
  activeSection: string
}>()

const emit = defineEmits<{
  close: []
  navigate: [section: string]
}>()

const navItems = [
  { id: 'overview', label: '数据概览', index: '01' },
  { id: 'trendSection', label: '趋势分析', index: '02' },
  { id: 'productSection', label: '商品洞察', index: '03' },
  { id: 'ordersSection', label: '订单流水', index: '04' },
]

function navigate(section: string) {
  emit('navigate', section)
  emit('close')
}

const dbMeta = computed(() => {
  if (!props.totalOrders) return '正在读取本地数据…'
  return `${formatNumber(props.totalOrders)} 笔订单 · 累计 ${formatCompactMoney(props.totalRevenue)}`
})
</script>

<template>
  <aside class="sidebar" id="sidebar" aria-label="侧边导航">
    <div class="sidebar-head">
      <a class="brand" href="#overview" aria-label="MORI COFFEE 首页">
        <span class="brand-mark" aria-hidden="true">
          <svg viewBox="0 0 32 32" fill="none">
            <path d="M7.5 22.5V10.2l8.5 7.1 8.5-7.1v12.3" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" />
            <path d="M11 24.3h10" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" />
          </svg>
        </span>
        <span class="brand-copy">
          <strong>MORI</strong>
          <small>COFFEE · 森屿</small>
        </span>
      </a>
      <button class="sidebar-close" type="button" aria-label="关闭导航" @click="emit('close')">
        <svg viewBox="0 0 20 20" fill="none" aria-hidden="true">
          <path d="M5 5l10 10M15 5L5 15" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" />
        </svg>
      </button>
    </div>

    <nav class="side-nav" aria-label="页面内导航">
      <p class="side-caption">经营面板 <span>WORKSPACE</span></p>
      <a
        v-for="item in navItems"
        :key="item.id"
        class="nav-item"
        :class="{ 'is-active': activeSection === item.id }"
        :href="`#${item.id}`"
        :aria-current="activeSection === item.id ? 'location' : undefined"
        @click="navigate(item.id)"
      >
        <span class="nav-icon" aria-hidden="true">
          <svg v-if="item.id === 'overview'" viewBox="0 0 20 20" fill="none">
            <path d="M3 10.5h4l2-5 2.4 9 1.8-6H17" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
          <svg v-else-if="item.id === 'trendSection'" viewBox="0 0 20 20" fill="none">
            <path d="M3.5 15.5V4.5M3.5 15.5h13M6 12l3.2-3.6 2.5 2 4.1-5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" />
          </svg>
          <svg v-else-if="item.id === 'productSection'" viewBox="0 0 20 20" fill="none">
            <path d="M4 16V9.5M10 16V4M16 16v-4.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
            <path d="M3 16.5h14" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
          </svg>
          <svg v-else viewBox="0 0 20 20" fill="none">
            <rect x="4" y="3" width="12" height="14" rx="2" stroke="currentColor" stroke-width="1.5" />
            <path d="M7 7h6M7 10h6M7 13h3.5" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
          </svg>
        </span>
        <span class="nav-text">{{ item.label }}</span>
        <span class="nav-index">{{ item.index }}</span>
      </a>
    </nav>

    <div class="side-stores">
      <p class="side-caption">门店网络 <span>{{ String(stores.length).padStart(2, '0') }}</span></p>
      <div class="side-store-list">
        <div v-if="!stores.length" class="side-store-skeleton" />
        <template v-else>
          <div
            v-for="store in stores"
            :key="store.id"
            class="side-store"
          >
            <span class="store-dot" :style="{ background: safeColor(store.accent) }" aria-hidden="true" />
            <span class="side-store-copy">
              <strong>{{ store.name }}</strong>
              <small>{{ store.address }}</small>
            </span>
            <span class="side-store-state">OPEN</span>
          </div>
        </template>
      </div>
    </div>

    <div class="sidebar-foot">
      <div class="db-status">
        <span class="status-dot" aria-hidden="true" />
        <span>SQLite 已连接</span>
        <span class="status-live">LOCAL</span>
      </div>
      <p class="db-meta">{{ dbMeta }}</p>
      <p class="sidebar-signature">MORI COFFEE · DATA DESK <span>© 2026</span></p>
    </div>
  </aside>
</template>
