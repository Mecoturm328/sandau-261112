package com.mori.dashboard.vo;

/**
 * 营收趋势点。
 */
public record TrendPointVO(String date, double revenue, long orders, long memberOrders) {
}
