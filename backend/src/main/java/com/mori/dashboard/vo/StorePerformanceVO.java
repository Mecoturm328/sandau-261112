package com.mori.dashboard.vo;

/**
 * 门店表现。
 */
public record StorePerformanceVO(
        long id,
        String name,
        String shortName,
        String address,
        String accent,
        long orders,
        double revenue,
        double avgTicket
) {
}
