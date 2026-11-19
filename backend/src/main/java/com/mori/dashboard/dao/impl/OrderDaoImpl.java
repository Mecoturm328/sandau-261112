package com.mori.dashboard.dao.impl;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.mori.dashboard.dao.OrderDao;
import com.mori.dashboard.dto.OrderQueryDTO;
import com.mori.dashboard.dto.stat.DailyStatDTO;
import com.mori.dashboard.dto.stat.HourlyStatDTO;
import com.mori.dashboard.dto.stat.KpiStatDTO;
import com.mori.dashboard.dto.stat.OrderDetailDTO;
import com.mori.dashboard.dto.stat.OrderItemLineDTO;
import com.mori.dashboard.dto.stat.OrderOverviewDTO;
import com.mori.dashboard.entity.Order;

@Repository
public class OrderDaoImpl implements OrderDao {

    /** group_concat 中商品名与数量之间、商品行之间的分隔符（ASCII 记录/单元分隔符，不会出现在商品名中）。 */
    private static final String FIELD_SEPARATOR = "\u001E";
    private static final String LINE_SEPARATOR = "\u001F";
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbc;

    public OrderDaoImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public long count() {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM orders", Long.class);
        return count == null ? 0 : count;
    }

    @Override
    public OrderOverviewDTO selectOverview() {
        return jdbc.queryForObject("""
                SELECT
                  MIN(substr(ordered_at, 1, 10)) AS min_date,
                  MAX(substr(ordered_at, 1, 10)) AS max_date,
                  COUNT(*) AS total_orders,
                  COALESCE(ROUND(SUM(total_amount), 2), 0) AS total_revenue
                FROM orders
                """, (rs, rowNum) -> new OrderOverviewDTO(
                rs.getString("min_date"),
                rs.getString("max_date"),
                rs.getLong("total_orders"),
                rs.getDouble("total_revenue")
        ));
    }

    @Override
    public KpiStatDTO selectKpiStat(OrderQueryDTO query) {
        OrderFilter filter = OrderFilter.of(query);
        return jdbc.queryForObject("""
                SELECT
                  COALESCE(ROUND(SUM(total_amount), 2), 0) AS revenue,
                  COUNT(*) AS orders,
                  COALESCE(SUM(item_count), 0) AS items,
                  COALESCE(SUM(is_member), 0) AS member_orders
                FROM orders o
                WHERE %s
                """.formatted(filter.clause()), (rs, rowNum) -> new KpiStatDTO(
                rs.getDouble("revenue"),
                rs.getLong("orders"),
                rs.getLong("items"),
                rs.getLong("member_orders")
        ), filter.params());
    }

    @Override
    public List<DailyStatDTO> selectDailyStats(OrderQueryDTO query) {
        OrderFilter filter = OrderFilter.of(query);
        return jdbc.query("""
                SELECT
                  substr(ordered_at, 1, 10) AS date,
                  ROUND(SUM(total_amount), 2) AS revenue,
                  COUNT(*) AS orders,
                  SUM(is_member) AS member_orders
                FROM orders o
                WHERE %s
                GROUP BY substr(ordered_at, 1, 10)
                ORDER BY date ASC
                """.formatted(filter.clause()), (rs, rowNum) -> new DailyStatDTO(
                rs.getString("date"),
                rs.getDouble("revenue"),
                rs.getLong("orders"),
                rs.getLong("member_orders")
        ), filter.params());
    }

    @Override
    public List<HourlyStatDTO> selectHourlyStats(OrderQueryDTO query) {
        OrderFilter filter = OrderFilter.of(query);
        return jdbc.query("""
                SELECT
                  CAST(substr(ordered_at, 12, 2) AS INTEGER) AS hour,
                  COUNT(*) AS orders,
                  ROUND(SUM(total_amount), 2) AS revenue
                FROM orders o
                WHERE %s
                GROUP BY hour
                ORDER BY hour
                """.formatted(filter.clause()), (rs, rowNum) -> new HourlyStatDTO(
                rs.getInt("hour"),
                rs.getLong("orders"),
                rs.getDouble("revenue")
        ), filter.params());
    }

    @Override
    public List<OrderDetailDTO> selectOrderDetails(OrderQueryDTO query, Integer limit) {
        OrderFilter filter = OrderFilter.of(query);
        String limitClause = limit == null ? "" : "LIMIT ?";
        Object[] params = limit == null ? filter.params() : filter.paramsWith(limit);
        return jdbc.query("""
                SELECT
                  o.order_no,
                  o.ordered_at,
                  s.name AS store_name,
                  s.short_name AS store_short_name,
                  s.accent AS store_accent,
                  o.total_amount,
                  o.item_count,
                  o.payment_method,
                  o.is_member,
                  (
                    SELECT group_concat(p.name || char(30) || oi.quantity, char(31))
                    FROM order_items oi
                    JOIN products p ON p.id = oi.product_id
                    WHERE oi.order_id = o.id
                  ) AS item_lines
                FROM orders o
                JOIN stores s ON s.id = o.store_id
                WHERE %s
                ORDER BY o.ordered_at DESC, o.id DESC
                %s
                """.formatted(filter.clause(), limitClause), (rs, rowNum) -> new OrderDetailDTO(
                rs.getString("order_no"),
                rs.getString("ordered_at"),
                rs.getString("store_name"),
                rs.getString("store_short_name"),
                rs.getString("store_accent"),
                rs.getDouble("total_amount"),
                rs.getLong("item_count"),
                rs.getString("payment_method"),
                rs.getInt("is_member") == 1,
                parseItemLines(rs.getString("item_lines"))
        ), params);
    }

    @Override
    public void batchInsert(List<Order> orders) {
        jdbc.batchUpdate(
                """
                INSERT INTO orders
                  (id, order_no, store_id, ordered_at, total_amount, item_count, payment_method, is_member)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                orders,
                500,
                (statement, order) -> {
                    statement.setLong(1, order.getId());
                    statement.setString(2, order.getOrderNo());
                    statement.setLong(3, order.getStoreId());
                    statement.setString(4, order.getOrderedAt().format(DATE_TIME_FORMAT));
                    statement.setDouble(5, order.getTotalAmount());
                    statement.setInt(6, order.getItemCount());
                    statement.setString(7, order.getPaymentMethod());
                    statement.setInt(8, order.isMember() ? 1 : 0);
                }
        );
    }

    @Override
    public void deleteAll() {
        jdbc.update("DELETE FROM orders");
    }

    private static List<OrderItemLineDTO> parseItemLines(String raw) {
        List<OrderItemLineDTO> lines = new ArrayList<>();
        if (raw == null || raw.isEmpty()) {
            return lines;
        }
        for (String line : raw.split(LINE_SEPARATOR)) {
            int index = line.lastIndexOf(FIELD_SEPARATOR);
            lines.add(new OrderItemLineDTO(
                    line.substring(0, index),
                    Integer.parseInt(line.substring(index + 1))
            ));
        }
        return lines;
    }
}
