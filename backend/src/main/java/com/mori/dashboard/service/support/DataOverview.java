package com.mori.dashboard.service.support;

import java.time.LocalDate;

/**
 * 数据集概览（已处理空库情况）。
 */
public record DataOverview(LocalDate minDate, LocalDate maxDate, long totalOrders, double totalRevenue) {
}
