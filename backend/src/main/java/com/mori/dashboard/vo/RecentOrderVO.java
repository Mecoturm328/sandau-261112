package com.mori.dashboard.vo;

/**
 * 最近订单。
 */
public record RecentOrderVO(
        String orderNo,
        String orderedAt,
        String store,
        String accent,
        double total,
        long itemCount,
        String payment,
        boolean isMember,
        String items
) {
}
