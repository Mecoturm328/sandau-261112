<script setup lang="ts">
import type { RangeInfo, Store } from '~/types/dashboard'

const props = defineProps<{
  start: string
  end: string
  storeId: string
  stores: Store[]
  preset: number | null
  bounds: RangeInfo | null
  loading: boolean
}>()

const emit = defineEmits<{
  'update:start': [value: string]
  'update:end': [value: string]
  'update:storeId': [value: string]
  preset: [days: number]
  apply: []
  storeChange: [value: string]
}>()

const presets = [7, 30, 90]

function onStoreChange(event: Event) {
  const value = (event.target as HTMLSelectElement).value
  emit('update:storeId', value)
  emit('storeChange', value)
}
</script>

<template>
  <section class="filter-bar" aria-label="筛选条件">
    <div class="period-switch" role="group" aria-label="快捷选择时间范围">
      <button
        v-for="days in presets"
        :key="days"
        class="period-button"
        :class="{ 'is-active': preset === days }"
        type="button"
        :aria-pressed="preset === days"
        :disabled="loading"
        @click="emit('preset', days)"
      >
        近 {{ days }} 天
      </button>
    </div>
    <span class="filter-divider" aria-hidden="true" />
    <div class="date-range">
      <label class="date-field">
        <span>开始</span>
        <input
          :value="start"
          type="date"
          aria-label="开始日期"
          :min="bounds?.minDate"
          :max="bounds?.maxDate"
          @input="emit('update:start', ($event.target as HTMLInputElement).value)"
          @keydown.enter.prevent="emit('apply')"
        >
      </label>
      <span class="date-dash" aria-hidden="true">—</span>
      <label class="date-field">
        <span>结束</span>
        <input
          :value="end"
          type="date"
          aria-label="结束日期"
          :min="bounds?.minDate"
          :max="bounds?.maxDate"
          @input="emit('update:end', ($event.target as HTMLInputElement).value)"
          @keydown.enter.prevent="emit('apply')"
        >
      </label>
    </div>
    <button class="query-button" type="button" :disabled="loading" @click="emit('apply')">
      查询
    </button>
    <div class="filter-spacer" />
    <label class="store-select-wrap">
      <span>门店</span>
      <select :value="storeId" aria-label="选择门店" @change="onStoreChange">
        <option value="">全部门店</option>
        <option v-for="store in stores" :key="store.id" :value="String(store.id)">
          {{ store.name }}
        </option>
      </select>
      <svg viewBox="0 0 16 16" fill="none" aria-hidden="true">
        <path d="M4 6l4 4 4-4" stroke="currentColor" stroke-width="1.4" stroke-linecap="round" stroke-linejoin="round" />
      </svg>
    </label>
  </section>
</template>
