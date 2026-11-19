<script setup lang="ts">
import type { RecentOrder } from '~/types/dashboard'
import { formatMoney, formatOrderStamp, safeColor } from '~/utils/format'

defineProps<{
  orders: RecentOrder[]
}>()

function shortOrder(orderNo: string): string {
  const parts = orderNo.split('-')
  return `${parts[1] || ''}·${parts[parts.length - 1] || orderNo}`
}
</script>

<template>
  <div class="table-wrap">
    <table v-if="orders.length" class="orders-table">
      <caption class="sr-only">最近订单流水</caption>
      <thead>
        <tr>
          <th scope="col">订单</th>
          <th scope="col">门店</th>
          <th scope="col">商品</th>
          <th scope="col" class="align-right">金额</th>
        </tr>
      </thead>
      <tbody>
        <tr v-for="order in orders" :key="order.orderNo">
          <td>
            <div class="order-meta" :title="`${order.orderNo} · ${order.payment}`">
              <strong>{{ shortOrder(order.orderNo) }}</strong>
              <small>{{ formatOrderStamp(order.orderedAt) }}</small>
            </div>
          </td>
          <td>
            <span class="order-store">
              <span class="store-dot" :style="{ background: safeColor(order.accent) }" aria-hidden="true" />
              {{ order.store }}
            </span>
          </td>
          <td>
            <span class="order-items" :title="order.items">
              <span class="order-items-text">{{ order.items }}</span>
              <span v-if="order.isMember" class="member-chip">会员</span>
            </span>
          </td>
          <td class="order-total">{{ formatMoney(order.total) }}</td>
        </tr>
      </tbody>
    </table>
    <div v-else class="empty-state empty-state--compact">
      <span class="empty-mark" aria-hidden="true">∅</span>
      <p>这个区间还没有订单，换个日期看看吧。</p>
    </div>
  </div>
</template>
