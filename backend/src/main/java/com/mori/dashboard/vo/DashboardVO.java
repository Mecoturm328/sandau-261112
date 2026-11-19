package com.mori.dashboard.vo;

import java.util.List;

/**
 * 看板接口 /api/dashboard 返回数据。
 */
public record DashboardVO(
        MetaVO meta,
        RangeVO range,
        Long storeId,
        List<StoreVO> stores,
        KpiVO kpis,
        List<TrendPointVO> trend,
        List<CategorySliceVO> categories,
        List<HourlyPointVO> hourly,
        List<TopProductVO> topProducts,
        List<StorePerformanceVO> storePerformance,
        List<RecentOrderVO> recentOrders
) {
}
