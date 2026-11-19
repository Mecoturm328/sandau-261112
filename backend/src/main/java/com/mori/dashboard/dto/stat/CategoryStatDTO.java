package com.mori.dashboard.dto.stat;

/**
 * 按商品品类聚合的销售数据。
 */
public record CategoryStatDTO(String category, double revenue, long quantity) {
}
