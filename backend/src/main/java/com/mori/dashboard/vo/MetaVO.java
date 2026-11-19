package com.mori.dashboard.vo;

/**
 * 数据元信息。
 */
public record MetaVO(String generatedAt, String source, long totalOrdersAllTime, double totalRevenueAllTime) {
}
