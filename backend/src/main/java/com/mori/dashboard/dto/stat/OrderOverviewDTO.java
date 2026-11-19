package com.mori.dashboard.dto.stat;

/**
 * 订单全量概览：日期范围与累计数据（日期可能为 null，表示无订单）。
 */
public record OrderOverviewDTO(String minDate, String maxDate, long totalOrders, double totalRevenue) {
}
