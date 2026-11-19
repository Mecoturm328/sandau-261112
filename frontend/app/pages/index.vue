<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { formatNumber } from '~/utils/format'

const {
  apiBase,
  data,
  loading,
  errorMessage,
  dateInputs,
  filters,
  toast,
  bounds,
  stores,
  heroRange,
  heroDays,
  updatedLabel,
  dailyAverage,
  storeScope,
  exportDisabled,
  dismissToast,
  loadDashboard,
  applyPreset,
  applyCustomRange,
  changeStore,
  exportCsv,
} = useDashboard()

const sidebarOpen = ref(false)
const activeSection = ref('overview')
const isMobile = ref(false)
let mediaQuery: MediaQueryList | null = null

const peakLabel = computed(() => {
  if (!data.value) return '—'
  const hourly = data.value.hourly
  const total = hourly.reduce((sum, item) => sum + item.orders, 0)
  if (!total) return '暂无高峰'
  const max = Math.max(...hourly.map((item) => item.orders))
  const peak = hourly.find((item) => item.orders === max)
  return peak ? `高峰 ${peak.label} · ${formatNumber(peak.orders)} 单` : '—'
})

const sectionIds = ['overview', 'trendSection', 'productSection', 'ordersSection']

function updateActiveSection() {
  let current = sectionIds[0]!
  for (const id of sectionIds) {
    const element = document.getElementById(id)
    if (!element) continue
    const top = element.getBoundingClientRect().top + window.scrollY
    if (top <= window.scrollY + 170) current = id
  }
  activeSection.value = current
}

function onMediaChange(event: MediaQueryListEvent | MediaQueryList) {
  isMobile.value = event.matches
  if (!event.matches) sidebarOpen.value = false
}

function closeSidebar() {
  sidebarOpen.value = false
}

function openSidebar() {
  sidebarOpen.value = true
}

function onNavigate(section: string) {
  activeSection.value = section
  closeSidebar()
}

function onKeydown(event: KeyboardEvent) {
  if (event.key === 'Escape' && sidebarOpen.value) closeSidebar()
}

watch(sidebarOpen, (open) => {
  if (!import.meta.client) return
  document.body.classList.toggle('nav-open', open)
})

watch(data, () => {
  nextTick(updateActiveSection)
})

onMounted(() => {
  mediaQuery = window.matchMedia('(max-width: 960px)')
  onMediaChange(mediaQuery)
  mediaQuery.addEventListener('change', onMediaChange)
  window.addEventListener('scroll', updateActiveSection, { passive: true })
  window.addEventListener('keydown', onKeydown)
  updateActiveSection()
})

onBeforeUnmount(() => {
  mediaQuery?.removeEventListener('change', onMediaChange)
  window.removeEventListener('scroll', updateActiveSection)
  window.removeEventListener('keydown', onKeydown)
  document.body.classList.remove('nav-open')
})
</script>

<template>
  <div class="app-shell">
    <div class="loading-bar" :class="{ 'is-active': loading }" aria-hidden="true" />

    <AppSidebar
      :stores="stores"
      :total-orders="data?.meta.totalOrdersAllTime ?? 0"
      :total-revenue="data?.meta.totalRevenueAllTime ?? 0"
      :active-section="activeSection"
      :inert="isMobile && !sidebarOpen"
      @close="closeSidebar"
      @navigate="onNavigate"
    />

    <div class="sidebar-scrim" :aria-hidden="!sidebarOpen" @click="closeSidebar" />

    <div class="page">
      <AppTopbar
        :updated-label="updatedLabel"
        :loading="loading"
        :sidebar-open="sidebarOpen"
        @toggle-sidebar="openSidebar"
        @refresh="loadDashboard"
      />

      <main class="content" :aria-busy="loading">
        <section id="overview" class="page-head" aria-labelledby="pageTitle">
          <div class="page-head-copy">
            <p class="eyebrow">
              <span class="eyebrow-line" aria-hidden="true" />OVERVIEW / 经营概览
            </p>
            <h1 id="pageTitle">每一杯咖啡，<br><span>都有迹可循。</span></h1>
            <p class="lede">
              从清晨的第一杯拿铁，到傍晚的最后一块巴斯克。看看三家门店最近卖得怎么样。
            </p>
          </div>
          <div class="date-stamp" aria-label="当前数据区间">
            <span class="stamp-icon" aria-hidden="true">
              <svg viewBox="0 0 32 32" fill="none">
                <circle cx="16" cy="16" r="12.5" stroke="currentColor" stroke-width="1.2" stroke-dasharray="2.5 3.5" />
                <path d="M16 9.5v7l4.2 2.4" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round" />
              </svg>
            </span>
            <span class="stamp-label">REPORTING PERIOD</span>
            <strong>{{ heroRange }}</strong>
            <span class="stamp-meta">{{ heroDays }}</span>
          </div>
        </section>

        <FilterBar
          v-model:start="dateInputs.start"
          v-model:end="dateInputs.end"
          v-model:store-id="filters.storeId"
          :stores="stores"
          :preset="filters.preset"
          :bounds="bounds"
          :loading="loading"
          @preset="applyPreset"
          @apply="applyCustomRange"
          @store-change="changeStore"
        />

        <template v-if="data">
          <KpiGrid :kpis="data.kpis" :trend="data.trend" />

          <section id="trendSection" class="dashboard-grid" aria-label="营收走势与品类构成">
            <article class="panel trend-panel">
              <header class="panel-head">
                <div>
                  <p class="panel-kicker">REVENUE TREND</p>
                  <h2>营收走势</h2>
                </div>
                <div class="panel-figure">
                  <span>日均营收</span>
                  <strong>{{ dailyAverage }}</strong>
                </div>
              </header>
              <div class="chart-legend">
                <span class="legend-item"><span class="legend-dot legend-dot--orange" aria-hidden="true" />营业额</span>
                <span class="legend-note">悬停查看每日详情</span>
              </div>
              <TrendChart :trend="data.trend" :range="data.range" />
            </article>

            <article class="panel category-panel">
              <header class="panel-head">
                <div>
                  <p class="panel-kicker">SALES MIX</p>
                  <h2>品类构成</h2>
                </div>
                <span class="panel-note">按营业额</span>
              </header>
              <CategoryDonut :categories="data.categories" />
            </article>
          </section>

          <section id="productSection" class="dashboard-grid dashboard-grid--secondary" aria-label="时段与门店表现">
            <article class="panel hourly-panel">
              <header class="panel-head">
                <div>
                  <p class="panel-kicker">DAILY RHYTHM</p>
                  <h2>一天的节奏</h2>
                </div>
                <span class="peak-badge">{{ peakLabel }}</span>
              </header>
              <p class="panel-subtitle">按到店时段统计订单量，找到属于门店的高峰。</p>
              <HourlyBars :hourly="data.hourly" />
            </article>

            <article class="panel store-panel">
              <header class="panel-head">
                <div>
                  <p class="panel-kicker">STORE RANKING</p>
                  <h2>门店表现</h2>
                </div>
                <span class="panel-note">{{ storeScope }}</span>
              </header>
              <StoreRanking :stores="data.storePerformance" />
            </article>
          </section>

          <section id="ordersSection" class="dashboard-grid dashboard-grid--bottom" aria-label="人气单品与最近订单">
            <article class="panel products-panel">
              <header class="panel-head">
                <div>
                  <p class="panel-kicker">TOP SELLERS</p>
                  <h2>人气单品</h2>
                </div>
                <span class="panel-note">按销量排序</span>
              </header>
              <ProductRanking :products="data.topProducts" />
            </article>

            <article class="panel orders-panel">
              <header class="panel-head">
                <div>
                  <p class="panel-kicker">RECENT ORDERS</p>
                  <h2>最近订单</h2>
                </div>
                <button class="export-button" type="button" :disabled="exportDisabled" @click="exportCsv">
                  <svg viewBox="0 0 18 18" fill="none" aria-hidden="true">
                    <path d="M9 2.8v8.1M5.8 8l3.2 3.2L12.2 8M3.5 12.3v1.9c0 .6.5 1 1 1h9c.6 0 1-.4 1-1v-1.9" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round" />
                  </svg>
                  <span>导出 CSV</span>
                </button>
              </header>
              <RecentOrders :orders="data.recentOrders" />
            </article>
          </section>

          <footer class="page-footer">
            <p>MORI COFFEE · 经营数据台 <span>Designed for morning people.</span></p>
            <p>本地 SQLite 数据库 · 演示数据 <span>· {{ formatNumber(data.meta.totalOrdersAllTime) }} 笔订单</span></p>
          </footer>
        </template>

        <template v-else-if="!errorMessage">
          <div class="kpi-grid" aria-hidden="true">
            <article v-for="index in 4" :key="index" class="kpi-card skeleton-card">
              <span class="skeleton-line skeleton-line--short" />
              <span class="skeleton-line skeleton-line--value" />
              <span class="skeleton-line skeleton-line--foot" />
            </article>
          </div>

          <div class="dashboard-grid" aria-hidden="true">
            <article class="panel skeleton-panel">
              <span class="skeleton-line skeleton-line--title" />
              <span class="skeleton-chart" />
            </article>
            <article class="panel skeleton-panel">
              <span class="skeleton-line skeleton-line--title" />
              <span class="skeleton-chart skeleton-chart--round" />
            </article>
          </div>
        </template>

        <div v-else class="error-state" role="alert">
          <span class="empty-mark" aria-hidden="true">!</span>
          <h2>暂时读不到数据</h2>
          <p>{{ errorMessage }}</p>
          <p class="error-hint">请确认后端服务已启动在 <code>{{ apiBase }}</code>，然后重试。</p>
          <button class="query-button" type="button" @click="loadDashboard">重新连接</button>
        </div>
      </main>
    </div>

    <ToastMessage :toast="toast" @close="dismissToast" />
  </div>
</template>
