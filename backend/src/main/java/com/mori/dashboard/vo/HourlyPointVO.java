package com.mori.dashboard.vo;

/**
 * 时段订单。
 */
public record HourlyPointVO(int hour, String label, long orders, double revenue) {
}
