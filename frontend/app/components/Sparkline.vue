<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  values: number[]
}>()

const geometry = computed(() => {
  const values = props.values
  if (!values.length) return null

  const width = 96
  const height = 30
  const padding = 2.5
  const min = Math.min(...values)
  const max = Math.max(...values)
  const span = max - min || 1
  const denominator = Math.max(1, values.length - 1)
  const points = values.map((value, index) => ({
    x: padding + (index / denominator) * (width - padding * 2),
    y: values.every((item) => item === max)
      ? height / 2
      : padding + (1 - ((value - min) / span)) * (height - padding * 2),
  }))

  let line = `M${points[0]!.x.toFixed(2)},${points[0]!.y.toFixed(2)}`
  for (let index = 1; index < points.length; index += 1) {
    line += ` L${points[index]!.x.toFixed(2)},${points[index]!.y.toFixed(2)}`
  }
  const last = points[points.length - 1]!
  const area = `${line} L${last.x.toFixed(2)},${height} L${points[0]!.x.toFixed(2)},${height} Z`
  return { width, height, line, area, last }
})
</script>

<template>
  <div class="sparkline" aria-hidden="true">
    <svg v-if="geometry" :viewBox="`0 0 ${geometry.width} ${geometry.height}`" preserveAspectRatio="none">
      <path :d="geometry.area" fill="currentColor" opacity="0.1" />
      <path
        :d="geometry.line"
        fill="none"
        stroke="currentColor"
        stroke-width="1.45"
        stroke-linecap="round"
        stroke-linejoin="round"
        vector-effect="non-scaling-stroke"
      />
      <circle :cx="geometry.last.x.toFixed(2)" :cy="geometry.last.y.toFixed(2)" r="1.7" fill="currentColor" />
    </svg>
  </div>
</template>
