package com.mori.dashboard.dto.stat;

/**
 * 指定区间内的订单汇总。
 */
public record KpiStatDTO(double revenue, long orders, long items, long memberOrders) {
}
