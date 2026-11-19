package com.mori.dashboard.dto.stat;

/**
 * 按日聚合的订单数据（只包含有订单的日期）。
 */
public record DailyStatDTO(String date, double revenue, long orders, long memberOrders) {
}
