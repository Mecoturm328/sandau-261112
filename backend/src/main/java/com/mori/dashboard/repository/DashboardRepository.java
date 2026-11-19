package com.mori.dashboard.repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.mori.dashboard.dto.DashboardResponse.CategorySlice;
import com.mori.dashboard.dto.DashboardResponse.HourlyPoint;
import com.mori.dashboard.dto.DashboardResponse.OrderExportRow;
import com.mori.dashboard.dto.DashboardResponse.RecentOrder;
import com.mori.dashboard.dto.DashboardResponse.Store;
import com.mori.dashboard.dto.DashboardResponse.StorePerformance;
import com.mori.dashboard.dto.DashboardResponse.TopProduct;
import com.mori.dashboard.dto.DashboardResponse.TrendPoint;

@Repository
public class DashboardRepository {

    private final JdbcTemplate jdbc;

    public DashboardRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public record Bounds(
            String minDate,
            String maxDate,
            long totalOrders,
            double totalRevenue
    ) {
    }

    public record KpiTotals(
            double revenue,
            long orders,
            long items,
            long memberOrders
    ) {
    }

    private record Filter(String clause, List<Object> params) {
    }

    private record CategoryRow(String name, double revenue, long quantity) {
    }

    public List<Store> getStores() {
        return jdbc.query("""
                SELECT id, name, short_name, address, accent
                FROM stores
                ORDER BY id
                """, (rs, rowNum) -> new Store(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("short_name"),
                rs.getString("address"),
                rs.getString("accent")
        ));
    }

    public boolean storeExists(long storeId) {
        Long count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM stores WHERE id = ?",
                Long.class,
                storeId
        );
        return count != null && count > 0;
    }

    public Bounds getBounds() {
        return jdbc.queryForObject("""
                SELECT
                  MIN(substr(ordered_at, 1, 10)) AS min_date,
                  MAX(substr(ordered_at, 1, 10)) AS max_date,
                  COUNT(*) AS total_orders,
                  COALESCE(SUM(total_amount), 0) AS total_revenue
                FROM orders
                """, (rs, rowNum) -> {
            String today = LocalDate.now().toString();
            String minDate = rs.getString("min_date");
            String maxDate = rs.getString("max_date");
            return new Bounds(
                    minDate == null ? today : minDate,
                    maxDate == null ? today : maxDate,
                    rs.getLong("total_orders"),
                    round(rs.getDouble("total_revenue"), 2)
            );
        });
    }

    public KpiTotals getKpis(LocalDate start, LocalDate end, Long storeId) {
        Filter filter = filter(start, end, storeId);
        return jdbc.queryForObject("""
                SELECT
                  COALESCE(SUM(total_amount), 0) AS revenue,
                  COUNT(*) AS orders,
                  COALESCE(SUM(item_count), 0) AS items,
                  COALESCE(SUM(is_member), 0) AS member_orders
                FROM orders o
                WHERE %s
                """.formatted(filter.clause()), (rs, rowNum) -> new KpiTotals(
                round(rs.getDouble("revenue"), 2),
                rs.getLong("orders"),
                rs.getLong("items"),
                rs.getLong("member_orders")
        ), filter.params().toArray());
    }

    public List<TrendPoint> getTrend(LocalDate start, LocalDate end, Long storeId) {
        Filter filter = filter(start, end, storeId);
        List<TrendPoint> rows = jdbc.query("""
                SELECT
                  substr(ordered_at, 1, 10) AS date,
                  ROUND(SUM(total_amount), 2) AS revenue,
                  COUNT(*) AS orders,
                  SUM(is_member) AS member_orders
                FROM orders o
                WHERE %s
                GROUP BY substr(ordered_at, 1, 10)
                ORDER BY date ASC
                """.formatted(filter.clause()), (rs, rowNum) -> new TrendPoint(
                rs.getString("date"),
                round(rs.getDouble("revenue"), 2),
                rs.getLong("orders"),
                rs.getLong("member_orders")
        ), filter.params().toArray());

        Map<String, TrendPoint> byDate = new HashMap<>();
        for (TrendPoint row : rows) {
            byDate.put(row.date(), row);
        }

        List<TrendPoint> filled = new ArrayList<>();
        LocalDate cursor = start;
        while (!cursor.isAfter(end)) {
            String date = cursor.toString();
            filled.add(byDate.getOrDefault(date, new TrendPoint(date, 0, 0, 0)));
            cursor = cursor.plusDays(1);
        }
        return filled;
    }

    public List<CategorySlice> getCategories(LocalDate start, LocalDate end, Long storeId) {
        Filter filter = filter(start, end, storeId);
        List<CategoryRow> rows = jdbc.query("""
                SELECT
                  p.category AS name,
                  ROUND(SUM(oi.subtotal), 2) AS revenue,
                  SUM(oi.quantity) AS quantity
                FROM order_items oi
                JOIN orders o ON o.id = oi.order_id
                JOIN products p ON p.id = oi.product_id
                WHERE %s
                GROUP BY p.category
                ORDER BY revenue DESC
                """.formatted(filter.clause()), (rs, rowNum) -> new CategoryRow(
                rs.getString("name"),
                round(rs.getDouble("revenue"), 2),
                rs.getLong("quantity")
        ), filter.params().toArray());

        double total = rows.stream().mapToDouble(CategoryRow::revenue).sum();
        return rows.stream()
                .map(row -> new CategorySlice(
                        row.name(),
                        row.revenue(),
                        row.quantity(),
                        total == 0 ? 0 : round(row.revenue() / total * 100, 1)
                ))
                .toList();
    }

    public List<HourlyPoint> getHourly(LocalDate start, LocalDate end, Long storeId) {
        Filter filter = filter(start, end, storeId);
        List<HourlyPoint> rows = jdbc.query("""
                SELECT
                  CAST(substr(ordered_at, 12, 2) AS INTEGER) AS hour,
                  COUNT(*) AS orders,
                  ROUND(SUM(total_amount), 2) AS revenue
                FROM orders o
                WHERE %s
                GROUP BY hour
                ORDER BY hour
                """.formatted(filter.clause()), (rs, rowNum) -> new HourlyPoint(
                rs.getInt("hour"),
                String.format("%02d:00", rs.getInt("hour")),
                rs.getLong("orders"),
                round(rs.getDouble("revenue"), 2)
        ), filter.params().toArray());

        Map<Integer, HourlyPoint> byHour = new HashMap<>();
        for (HourlyPoint row : rows) {
            byHour.put(row.hour(), row);
        }

        List<HourlyPoint> filled = new ArrayList<>();
        for (int hour = 8; hour <= 20; hour += 1) {
            filled.add(byHour.getOrDefault(
                    hour,
                    new HourlyPoint(hour, String.format("%02d:00", hour), 0, 0)
            ));
        }
        return filled;
    }

    public List<TopProduct> getTopProducts(LocalDate start, LocalDate end, Long storeId) {
        Filter filter = filter(start, end, storeId);
        return jdbc.query("""
                SELECT
                  p.name,
                  p.category,
                  SUM(oi.quantity) AS quantity,
                  ROUND(SUM(oi.subtotal), 2) AS revenue
                FROM order_items oi
                JOIN orders o ON o.id = oi.order_id
                JOIN products p ON p.id = oi.product_id
                WHERE %s
                GROUP BY p.id, p.name, p.category
                ORDER BY quantity DESC, revenue DESC
                LIMIT 6
                """.formatted(filter.clause()), (rs, rowNum) -> new TopProduct(
                rs.getString("name"),
                rs.getString("category"),
                rs.getLong("quantity"),
                round(rs.getDouble("revenue"), 2)
        ), filter.params().toArray());
    }

    public List<StorePerformance> getStorePerformance(LocalDate start, LocalDate end, Long storeId) {
        List<Object> params = new ArrayList<>();
        params.add(start + " 00:00:00");
        params.add(end.plusDays(1) + " 00:00:00");

        String storeFilter = "";
        if (storeId != null) {
            storeFilter = "WHERE s.id = ?";
            params.add(storeId);
        }

        return jdbc.query("""
                SELECT
                  s.id,
                  s.name,
                  s.short_name,
                  s.address,
                  s.accent,
                  COUNT(o.id) AS orders,
                  COALESCE(ROUND(SUM(o.total_amount), 2), 0) AS revenue,
                  COALESCE(ROUND(SUM(o.total_amount) / NULLIF(COUNT(o.id), 0), 2), 0) AS avg_ticket
                FROM stores s
                LEFT JOIN orders o
                  ON o.store_id = s.id
                  AND o.ordered_at >= ?
                  AND o.ordered_at < ?
                %s
                GROUP BY s.id, s.name, s.short_name, s.address, s.accent
                ORDER BY revenue DESC, s.id ASC
                """.formatted(storeFilter), (rs, rowNum) -> new StorePerformance(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("short_name"),
                rs.getString("address"),
                rs.getString("accent"),
                rs.getLong("orders"),
                round(rs.getDouble("revenue"), 2),
                round(rs.getDouble("avg_ticket"), 2)
        ), params.toArray());
    }

    public List<RecentOrder> getRecentOrders(LocalDate start, LocalDate end, Long storeId) {
        Filter filter = filter(start, end, storeId);
        return jdbc.query("""
                SELECT
                  o.order_no,
                  o.ordered_at,
                  s.short_name AS store,
                  s.accent,
                  o.total_amount AS total,
                  o.item_count,
                  o.payment_method,
                  o.is_member,
                  (
                    SELECT group_concat(p.name || ' ×' || oi.quantity, ' · ')
                    FROM order_items oi
                    JOIN products p ON p.id = oi.product_id
                    WHERE oi.order_id = o.id
                  ) AS items
                FROM orders o
                JOIN stores s ON s.id = o.store_id
                WHERE %s
                ORDER BY o.ordered_at DESC, o.id DESC
                LIMIT 7
                """.formatted(filter.clause()), (rs, rowNum) -> new RecentOrder(
                rs.getString("order_no"),
                rs.getString("ordered_at"),
                rs.getString("store"),
                rs.getString("accent"),
                round(rs.getDouble("total"), 2),
                rs.getLong("item_count"),
                rs.getString("payment_method"),
                rs.getInt("is_member") == 1,
                rs.getString("items") == null ? "" : rs.getString("items")
        ), filter.params().toArray());
    }

    public List<OrderExportRow> getExportRows(LocalDate start, LocalDate end, Long storeId) {
        Filter filter = filter(start, end, storeId);
        return jdbc.query("""
                SELECT
                  o.order_no,
                  o.ordered_at,
                  s.name AS store,
                  (
                    SELECT group_concat(p.name || ' ×' || oi.quantity, ' / ')
                    FROM order_items oi
                    JOIN products p ON p.id = oi.product_id
                    WHERE oi.order_id = o.id
                  ) AS items,
                  o.item_count,
                  o.total_amount AS total,
                  o.payment_method,
                  o.is_member
                FROM orders o
                JOIN stores s ON s.id = o.store_id
                WHERE %s
                ORDER BY o.ordered_at DESC, o.id DESC
                """.formatted(filter.clause()), (rs, rowNum) -> new OrderExportRow(
                rs.getString("order_no"),
                rs.getString("ordered_at"),
                rs.getString("store"),
                rs.getString("items") == null ? "" : rs.getString("items"),
                rs.getLong("item_count"),
                round(rs.getDouble("total"), 2),
                rs.getString("payment_method"),
                rs.getInt("is_member") == 1
        ), filter.params().toArray());
    }

    private Filter filter(LocalDate start, LocalDate end, Long storeId) {
        StringBuilder clause = new StringBuilder("o.ordered_at >= ? AND o.ordered_at < ?");
        List<Object> params = new ArrayList<>();
        params.add(start + " 00:00:00");
        params.add(end.plusDays(1) + " 00:00:00");
        if (storeId != null) {
            clause.append(" AND o.store_id = ?");
            params.add(storeId);
        }
        return new Filter(clause.toString(), params);
    }

    private static double round(double value, int digits) {
        double factor = Math.pow(10, digits);
        return Math.round((value + 1e-9) * factor) / factor;
    }
}
