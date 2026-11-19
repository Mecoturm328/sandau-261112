package com.mori.dashboard.dto.stat;

/**
 * 按小时聚合的订单数据（只包含有订单的小时）。
 */
public record HourlyStatDTO(int hour, long orders, double revenue) {
}
