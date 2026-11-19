<script setup lang="ts">
import { computed } from 'vue'
import type { TopProduct } from '~/types/dashboard'
import { formatMoney, formatNumber } from '~/utils/format'

const props = defineProps<{
  products: TopProduct[]
}>()

const maxQuantity = computed(() => Math.max(0, ...props.products.map((product) => product.quantity)))

function width(quantity: number): number {
  return maxQuantity.value ? Math.max(3, (quantity / maxQuantity.value) * 100) : 0
}

function unit(category: string): string {
  return category === '咖啡' || category === '非咖啡' ? '杯' : '份'
}
</script>

<template>
  <div class="product-ranking">
    <div v-if="!products.length" class="ranking-empty">
      这个时段还没有售出商品
    </div>
    <div v-for="(product, index) in products" v-else :key="product.name" class="product-row">
      <span class="product-rank" aria-hidden="true">{{ String(index + 1).padStart(2, '0') }}</span>
      <div class="product-main">
        <div class="product-name-line">
          <strong>{{ product.name }}</strong>
          <span class="category-pill">{{ product.category }}</span>
        </div>
        <div class="product-track" aria-hidden="true">
          <span :style="{ width: `${width(product.quantity).toFixed(2)}%` }" />
        </div>
      </div>
      <div class="product-numbers">
        <strong>{{ formatNumber(product.quantity) }} {{ unit(product.category) }}</strong>
        <small>{{ formatMoney(product.revenue) }}</small>
      </div>
    </div>
  </div>
</template>
