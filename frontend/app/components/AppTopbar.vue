<script setup lang="ts">
defineProps<{
  updatedLabel: string
  loading: boolean
  sidebarOpen: boolean
}>()

defineEmits<{
  toggleSidebar: []
  refresh: []
}>()
</script>

<template>
  <header class="topbar">
    <div class="topbar-left">
      <button
        class="icon-button menu-button"
        type="button"
        aria-label="打开导航"
        aria-controls="sidebar"
        :aria-expanded="sidebarOpen"
        @click="$emit('toggleSidebar')"
      >
        <svg viewBox="0 0 20 20" fill="none" aria-hidden="true">
          <path d="M3 5.5h14M3 10h14M3 14.5h9" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" />
        </svg>
      </button>
      <div class="breadcrumb" aria-label="当前位置">
        <span>门店经营</span>
        <span class="breadcrumb-slash" aria-hidden="true">/</span>
        <strong>数据概览</strong>
      </div>
    </div>
    <div class="topbar-right">
      <span class="updated-at">{{ updatedLabel }}</span>
      <button
        class="refresh-button"
        :class="{ 'is-loading': loading }"
        type="button"
        :disabled="loading"
        @click="$emit('refresh')"
      >
        <svg viewBox="0 0 20 20" fill="none" aria-hidden="true">
          <path d="M16 7.2A6.4 6.4 0 1 0 16.2 12" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" />
          <path d="M16.5 3.8v3.8h-3.8" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round" />
        </svg>
        <span>刷新</span>
      </button>
    </div>
  </header>
</template>
