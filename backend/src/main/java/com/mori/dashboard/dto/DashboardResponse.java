package com.mori.dashboard.dto;

import java.util.List;

public record DashboardResponse(
        Meta meta,
        RangeInfo range,
        Long storeId,
        List<Store> stores,
        Kpis kpis,
        List<TrendPoint> trend,
        List<CategorySlice> categories,
        List<HourlyPoint> hourly,
        List<TopProduct> topProducts,
        List<StorePerformance> storePerformance,
        List<RecentOrder> recentOrders
) {

    public record Meta(
            String generatedAt,
            String source,
            long totalOrdersAllTime,
            double totalRevenueAllTime
    ) {
    }

    public record RangeInfo(
            String start,
            String end,
            int days,
            String minDate,
            String maxDate,
            PreviousPeriod previous
    ) {
    }

    public record PreviousPeriod(String start, String end) {
    }

    public record Store(
            long id,
            String name,
            String shortName,
            String address,
            String accent
    ) {
    }

    public record Kpis(
            double revenue,
            long orders,
            long items,
            double avgTicket,
            double memberShare,
            long memberOrders,
            PreviousKpis previous,
            Changes change
    ) {
    }

    public record PreviousKpis(
            double revenue,
            long orders,
            double avgTicket,
            double memberShare
    ) {
    }

    public record Changes(
            Double revenue,
            Double orders,
            Double avgTicket,
            Double memberShare
    ) {
    }

    public record TrendPoint(
            String date,
            double revenue,
            long orders,
            long memberOrders
    ) {
    }

    public record CategorySlice(
            String name,
            double revenue,
            long quantity,
            double share
    ) {
    }

    public record HourlyPoint(
            int hour,
            String label,
            long orders,
            double revenue
    ) {
    }

    public record TopProduct(
            String name,
            String category,
            long quantity,
            double revenue
    ) {
    }

    public record StorePerformance(
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

    public record RecentOrder(
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

    public record OrderExportRow(
            String orderNo,
            String orderedAt,
            String store,
            String items,
            long itemCount,
            double total,
            String payment,
            boolean isMember
    ) {
    }
}
