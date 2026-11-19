package com.mori.dashboard.vo;

/**
 * 核心指标。
 */
public record KpiVO(
        double revenue,
        long orders,
        long items,
        double avgTicket,
        double memberShare,
        long memberOrders,
        KpiSnapshotVO previous,
        KpiChangeVO change
) {
}
