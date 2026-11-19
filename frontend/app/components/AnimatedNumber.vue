<script setup lang="ts">
import { onBeforeUnmount, ref, watch } from 'vue'

const props = defineProps<{
  value: number
  formatter: (value: number) => string
}>()

const display = ref(props.formatter(0))
let currentValue = 0
let frame = 0

function animateTo(target: number) {
  const end = Number(target) || 0
  const start = currentValue
  currentValue = end

  if (frame) cancelAnimationFrame(frame)
  if (!import.meta.client || window.matchMedia('(prefers-reduced-motion: reduce)').matches || start === end) {
    display.value = props.formatter(end)
    return
  }

  const startedAt = performance.now()
  const duration = 620
  const tick = (now: number) => {
    const progress = Math.min(1, (now - startedAt) / duration)
    const eased = 1 - (1 - progress) ** 3
    display.value = props.formatter(start + (end - start) * eased)
    if (progress < 1) {
      frame = requestAnimationFrame(tick)
    }
  }
  frame = requestAnimationFrame(tick)
}

watch(() => props.value, animateTo, { immediate: true })

onBeforeUnmount(() => {
  if (frame) cancelAnimationFrame(frame)
})
</script>

<template>
  <span>{{ display }}</span>
</template>
