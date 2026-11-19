<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import type { RangeInfo, TrendPoint } from '~/types/dashboard'
import {
  formatAxisMoney,
  formatChineseDate,
  formatMoney,
  formatMonthDay,
  formatNumber,
  formatRange,
} from '~/utils/format'

const props = defineProps<{
  trend: TrendPoint[]
  range: RangeInfo
}>()

const chartEl = ref<HTMLDivElement | null>(null)
const size = reactive({ width: 680, height: 273 })
const hoverIndex = ref<number | null>(null)
let resizeObserver: ResizeObserver | null = null

function measure() {
  if (!chartEl.value) return
  size.width = Math.max(300, Math.round(chartEl.value.clientWidth || 680))
  size.height = Math.max(210, Math.round(chartEl.value.clientHeight || 273))
}

onMounted(() => {
  measure()
  resizeObserver = new ResizeObserver(measure)
  if (chartEl.value) resizeObserver.observe(chartEl.value)
})

onBeforeUnmount(() => {
  resizeObserver?.disconnect()
})

watch(() => props.trend, () => {
  hoverIndex.value = null
})

const totalRevenue = computed(() => props.trend.reduce((sum, item) => sum + item.revenue, 0))
const totalOrders = computed(() => props.trend.reduce((sum, item) => sum + item.orders, 0))
const hasData = computed(() => props.trend.length > 0 && totalRevenue.value > 0)

function niceAxisMaximum(value: number): number {
  if (!value) return 100
  const rough = value / 4
  const power = 10 ** Math.floor(Math.log10(rough))
  const normalized = rough / power
  const niceSteps = [1, 1.5, 2, 2.5, 3, 4, 5, 6, 8, 10]
  const step = niceSteps.find((candidate) => normalized <= candidate) || 10
  return step * power * 4
}

function smoothPath(points: Array<{ x: number, y: number }>): string {
  if (!points.length) return ''
  if (points.length === 1) return `M${points[0]!.x},${points[0]!.y}`
  let path = `M${points[0]!.x.toFixed(2)},${points[0]!.y.toFixed(2)}`
  for (let index = 1; index < points.length; index += 1) {
    const previous = points[index - 1]!
    const current = points[index]!
    const middleX = (previous.x + current.x) / 2
    path += ` C${middleX.toFixed(2)},${previous.y.toFixed(2)} ${middleX.toFixed(2)},${current.y.toFixed(2)} ${current.x.toFixed(2)},${current.y.toFixed(2)}`
  }
  return path
}

const geometry = computed(() => {
  const width = size.width
  const height = size.height
  const margin = {
    top: 17,
    right: width < 540 ? 12 : 18,
    bottom: 29,
    left: width < 540 ? 44 : 52,
  }
  const plotWidth = width - margin.left - margin.right
  const plotHeight = height - margin.top - margin.bottom
  const baseY = margin.top + plotHeight
  const maxRevenue = Math.max(...props.trend.map((item) => item.revenue))
  const yMax = niceAxisMaximum(maxRevenue)
  const denominator = Math.max(1, props.trend.length - 1)
  const xAt = (index: number) => margin.left + (index / denominator) * plotWidth
  const yAt = (value: number) => margin.top + plotHeight - (value / yMax) * plotHeight

  const points = props.trend.map((item, index) => ({
    x: xAt(index),
    y: yAt(item.revenue),
    item,
  }))

  const grid = Array.from({ length: 5 }, (_, step) => {
    const y = margin.top + (step / 4) * plotHeight
    return {
      y,
      value: yMax * (1 - step / 4),
      isBase: step === 4,
    }
  })

  const maxTicks = width < 520 ? 4 : (props.trend.length <= 14 ? 7 : 6)
  const tickStep = Math.max(1, Math.ceil((props.trend.length - 1) / Math.max(1, maxTicks - 1)))
  const tickIndices: number[] = []
  for (let index = 0; index < props.trend.length; index += tickStep) tickIndices.push(index)
  const lastIndex = props.trend.length - 1
  if (tickIndices[tickIndices.length - 1] !== lastIndex && lastIndex - tickIndices[tickIndices.length - 1]! > tickStep / 2) {
    tickIndices.push(lastIndex)
  }
  const xLabels = tickIndices.map((index) => ({
    x: xAt(index),
    label: formatMonthDay(props.trend[index]!.date),
    anchor: index === lastIndex ? 'end' : index === 0 ? 'start' : 'middle',
  }))

  const line = smoothPath(points)
  const lastPoint = points[points.length - 1]
  const area = lastPoint
    ? `${line} L${lastPoint.x.toFixed(2)},${baseY.toFixed(2)} L${points[0]!.x.toFixed(2)},${baseY.toFixed(2)} Z`
    : ''

  return {
    width,
    height,
    margin,
    plotWidth,
    plotHeight,
    baseY,
    points,
    grid,
    xLabels,
    line,
    area,
    lastPoint,
  }
})

const ariaLabel = computed(() => {
  if (!hasData.value) return '所选时段没有营业额数据'
  return `${formatRange(props.range.start, props.range.end)}每日营业额走势，总营业额 ${formatMoney(totalRevenue.value)}，总订单 ${formatNumber(totalOrders.value)} 单。`
})

const hovered = computed(() => {
  if (hoverIndex.value === null || !geometry.value.points[hoverIndex.value]) return null
  return geometry.value.points[hoverIndex.value]!
})

const tooltip = computed(() => {
  if (!hovered.value) return null
  const point = hovered.value
  const width = geometry.value.width
  const height = geometry.value.height
  const tooltipWidth = 168
  const left = Math.max(4, Math.min(width - tooltipWidth - 4, point.x - tooltipWidth / 2))
  let top = point.y - 91
  let below = false
  if (top < 4) {
    top = point.y + 15
    below = true
  }
  top = Math.max(4, Math.min(height - 80, top))
  const arrowLeft = Math.max(13, Math.min(tooltipWidth - 13, point.x - left))
  return {
    left,
    top,
    below,
    arrowLeft,
    point,
    style: {
      left: `${left}px`,
      top: `${top}px`,
      '--arrow-left': `${arrowLeft}px`,
    } as Record<string, string>,
  }
})

function onPointerMove(event: PointerEvent) {
  if (!chartEl.value || !hasData.value) return
  const rect = chartEl.value.getBoundingClientRect()
  const relativeX = event.clientX - rect.left
  const { margin, plotWidth } = geometry.value
  if (relativeX < margin.left - 4 || relativeX > margin.left + plotWidth + 4) {
    hoverIndex.value = null
    return
  }
  hoverIndex.value = Math.max(
    0,
    Math.min(
      props.trend.length - 1,
      Math.round(((relativeX - margin.left) / plotWidth) * (props.trend.length - 1)),
    ),
  )
}

function onPointerLeave() {
  hoverIndex.value = null
}
</script>

<template>
  <div class="trend-chart-wrap">
    <div
      ref="chartEl"
      class="trend-chart"
      role="img"
      :aria-label="ariaLabel"
      @pointermove="onPointerMove"
      @pointerleave="onPointerLeave"
    >
      <svg
        v-if="hasData"
        class="trend-svg"
        :viewBox="`0 0 ${geometry.width} ${geometry.height}`"
        aria-hidden="true"
      >
        <defs>
          <linearGradient id="trendAreaGradient" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stop-color="#cf6d3f" stop-opacity="0.24" />
            <stop offset="100%" stop-color="#cf6d3f" stop-opacity="0" />
          </linearGradient>
        </defs>
        <g v-for="(line, index) in geometry.grid" :key="index">
          <line
            :class="line.isBase ? 'chart-grid-base' : 'chart-grid'"
            :x1="geometry.margin.left"
            :y1="line.y"
            :x2="geometry.margin.left + geometry.plotWidth"
            :y2="line.y"
          />
          <text
            class="chart-axis-label"
            :x="geometry.margin.left - 9"
            :y="line.y + 3"
            text-anchor="end"
          >{{ formatAxisMoney(line.value) }}</text>
        </g>
        <text
          v-for="tick in geometry.xLabels"
          :key="tick.label + tick.x"
          class="chart-axis-label"
          :x="tick.x"
          :y="geometry.height - 8"
          :text-anchor="tick.anchor"
        >{{ tick.label }}</text>
        <path
          class="chart-area"
          :d="geometry.area"
          fill="url(#trendAreaGradient)"
        />
        <path class="trend-path" :d="geometry.line" pathLength="1" />
        <template v-if="geometry.lastPoint">
          <circle
            class="trend-last-halo"
            :cx="geometry.lastPoint.x"
            :cy="geometry.lastPoint.y"
            r="8"
          />
          <circle
            class="trend-last-dot"
            :cx="geometry.lastPoint.x"
            :cy="geometry.lastPoint.y"
            r="3.4"
          />
        </template>
        <g v-if="hovered" class="trend-hover">
          <line
            class="chart-hover-line"
            :x1="hovered.x"
            :y1="geometry.margin.top"
            :x2="hovered.x"
            :y2="geometry.baseY"
          />
          <circle class="chart-hover-dot" :cx="hovered.x" :cy="hovered.y" r="4" />
        </g>
        <rect
          class="chart-hit-area"
          :x="geometry.margin.left"
          :y="geometry.margin.top"
          :width="geometry.plotWidth"
          :height="geometry.plotHeight"
        />
      </svg>
      <div v-else class="empty-chart">
        <span class="empty-mark" aria-hidden="true">∅</span>
        <p>这个时段还没有营业记录</p>
      </div>
    </div>

    <div
      v-if="tooltip && hovered"
      class="chart-tooltip"
      :class="{ 'is-below': tooltip.below }"
      :style="tooltip.style"
    >
      <span class="tooltip-date">{{ formatChineseDate(hovered.item.date) }}</span>
      <div class="tooltip-row">
        <span>营业额</span>
        <strong>{{ formatMoney(hovered.item.revenue) }}</strong>
      </div>
      <div class="tooltip-row">
        <span>订单</span>
        <strong>{{ formatNumber(hovered.item.orders) }} 单</strong>
      </div>
    </div>
  </div>
</template>
