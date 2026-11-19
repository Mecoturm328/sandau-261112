<script setup lang="ts">
import { computed } from 'vue'
import type { Kpis, TrendPoint } from '~/types/dashboard'
import {
  changeClass,
  changeLabel,
  formatChange,
  formatDecimal,
  formatNumber,
} from '~/utils/format'

const props = defineProps<{
  kpis: Kpis
  trend: TrendPoint[]
}>()

const avgTicketSeries = computed(() => props.trend.map((item) => (
  item.orders ? item.revenue / item.orders : 0
)))

const memberShareSeries = computed(() => props.trend.map((item) => (
  item.orders ? (item.memberOrders / item.orders) * 100 : 0
)))

function pillClass(value: number | null | undefined) {
  return changeClass(value)
}

function pillLabel(value: number | null | undefined, unit = '%') {
  return changeLabel(value, unit)
}
</script>

<template>
  <section class="kpi-grid" aria-label="核心经营指标">
    <article class="kpi-card kpi-card--featured">
      <div class="kpi-head">
        <span class="kpi-label">营业额</span>
        <span class="kpi-index">01</span>
      </div>
      <div class="kpi-value">
        <span class="kpi-currency">¥</span>
        <strong><AnimatedNumber :value="kpis.revenue" :formatter="formatNumber" /></strong>
      </div>
      <div class="kpi-foot">
        <div class="kpi-change">
          <span
            class="change-pill"
            :class="pillClass(kpis.change.revenue)"
            :title="pillLabel(kpis.change.revenue)"
            :aria-label="pillLabel(kpis.change.revenue)"
          >{{ formatChange(kpis.change.revenue) }}</span>
          <span class="change-caption">较上期</span>
        </div>
        <Sparkline :values="trend.map((item) => item.revenue)" />
      </div>
    </article>

    <article class="kpi-card">
      <div class="kpi-head">
        <span class="kpi-label">订单量</span>
        <span class="kpi-index">02</span>
      </div>
      <div class="kpi-value">
        <strong><AnimatedNumber :value="kpis.orders" :formatter="formatNumber" /></strong>
        <span class="kpi-suffix">单</span>
      </div>
      <div class="kpi-foot">
        <div class="kpi-change">
          <span
            class="change-pill"
            :class="pillClass(kpis.change.orders)"
            :title="pillLabel(kpis.change.orders)"
            :aria-label="pillLabel(kpis.change.orders)"
          >{{ formatChange(kpis.change.orders) }}</span>
          <span class="change-caption">较上期</span>
        </div>
        <Sparkline :values="trend.map((item) => item.orders)" />
      </div>
    </article>

    <article class="kpi-card">
      <div class="kpi-head">
        <span class="kpi-label">客单价</span>
        <span class="kpi-index">03</span>
      </div>
      <div class="kpi-value">
        <span class="kpi-currency">¥</span>
        <strong><AnimatedNumber :value="kpis.avgTicket" :formatter="formatDecimal" /></strong>
      </div>
      <div class="kpi-foot">
        <div class="kpi-change">
          <span
            class="change-pill"
            :class="pillClass(kpis.change.avgTicket)"
            :title="pillLabel(kpis.change.avgTicket)"
            :aria-label="pillLabel(kpis.change.avgTicket)"
          >{{ formatChange(kpis.change.avgTicket) }}</span>
          <span class="change-caption">较上期</span>
        </div>
        <Sparkline :values="avgTicketSeries" />
      </div>
    </article>

    <article class="kpi-card">
      <div class="kpi-head">
        <span class="kpi-label">会员订单占比</span>
        <span class="kpi-index">04</span>
      </div>
      <div class="kpi-value">
        <strong><AnimatedNumber :value="kpis.memberShare" :formatter="formatDecimal" /></strong>
        <span class="kpi-suffix">%</span>
      </div>
      <div class="kpi-foot">
        <div class="kpi-change">
          <span
            class="change-pill"
            :class="pillClass(kpis.change.memberShare)"
            :title="pillLabel(kpis.change.memberShare, 'pp')"
            :aria-label="pillLabel(kpis.change.memberShare, 'pp')"
          >{{ formatChange(kpis.change.memberShare, 'pp') }}</span>
          <span class="change-caption">较上期</span>
        </div>
        <Sparkline :values="memberShareSeries" />
      </div>
    </article>
  </section>
</template>
