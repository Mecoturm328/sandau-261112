package com.mori.dashboard.dto.stat;

/**
 * 单品销售数据。
 */
public record ProductSalesDTO(String name, String category, long quantity, double revenue) {
}
