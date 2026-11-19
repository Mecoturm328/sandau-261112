package com.mori.dashboard.dao.impl;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.mori.dashboard.dao.OrderItemDao;
import com.mori.dashboard.dto.OrderQueryDTO;
import com.mori.dashboard.dto.stat.CategoryStatDTO;
import com.mori.dashboard.dto.stat.ProductSalesDTO;
import com.mori.dashboard.entity.OrderItem;

@Repository
public class OrderItemDaoImpl implements OrderItemDao {

    private final JdbcTemplate jdbc;

    public OrderItemDaoImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<CategoryStatDTO> selectCategoryStats(OrderQueryDTO query) {
        OrderFilter filter = OrderFilter.of(query);
        return jdbc.query("""
                SELECT
                  p.category,
                  ROUND(SUM(oi.subtotal), 2) AS revenue,
                  SUM(oi.quantity) AS quantity
                FROM order_items oi
                JOIN orders o ON o.id = oi.order_id
                JOIN products p ON p.id = oi.product_id
                WHERE %s
                GROUP BY p.category
                ORDER BY revenue DESC
                """.formatted(filter.clause()), (rs, rowNum) -> new CategoryStatDTO(
                rs.getString("category"),
                rs.getDouble("revenue"),
                rs.getLong("quantity")
        ), filter.params());
    }

    @Override
    public List<ProductSalesDTO> selectTopProducts(OrderQueryDTO query, int limit) {
        OrderFilter filter = OrderFilter.of(query);
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
                LIMIT ?
                """.formatted(filter.clause()), (rs, rowNum) -> new ProductSalesDTO(
                rs.getString("name"),
                rs.getString("category"),
                rs.getLong("quantity"),
                rs.getDouble("revenue")
        ), filter.paramsWith(limit));
    }

    @Override
    public void batchInsert(List<OrderItem> items) {
        jdbc.batchUpdate(
                """
                INSERT INTO order_items
                  (order_id, product_id, quantity, unit_price, subtotal)
                VALUES (?, ?, ?, ?, ?)
                """,
                items,
                500,
                (statement, item) -> {
                    statement.setLong(1, item.getOrderId());
                    statement.setLong(2, item.getProductId());
                    statement.setInt(3, item.getQuantity());
                    statement.setDouble(4, item.getUnitPrice());
                    statement.setDouble(5, item.getSubtotal());
                }
        );
    }

    @Override
    public void deleteAll() {
        jdbc.update("DELETE FROM order_items");
    }
}
