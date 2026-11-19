<script setup lang="ts">
import type { ToastState } from '~/types/dashboard'

defineProps<{
  toast: ToastState
}>()

defineEmits<{
  close: []
}>()
</script>

<template>
  <div
    v-if="toast.visible"
    class="toast"
    :class="{ 'is-error': toast.type === 'error' }"
    role="status"
    aria-live="polite"
    @click="$emit('close')"
  >
    <span class="toast-dot" aria-hidden="true" />
    <span>{{ toast.message }}</span>
  </div>
</template>
