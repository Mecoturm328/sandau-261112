package com.mori.dashboard.dto.stat;

/**
 * 门店销售数据。
 */
public record StoreSalesDTO(long storeId, String name, String shortName, String address, String accent, long orders, double revenue) {
}
