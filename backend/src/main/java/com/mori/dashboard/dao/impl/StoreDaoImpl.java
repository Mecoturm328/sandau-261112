package com.mori.dashboard.dao.impl;

import java.util.ArrayList;
import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import com.mori.dashboard.dao.StoreDao;
import com.mori.dashboard.dto.OrderQueryDTO;
import com.mori.dashboard.dto.stat.StoreSalesDTO;
import com.mori.dashboard.entity.Store;

@Repository
public class StoreDaoImpl implements StoreDao {

    private static final RowMapper<Store> STORE_MAPPER = (rs, rowNum) -> new Store(
            rs.getLong("id"),
            rs.getString("name"),
            rs.getString("short_name"),
            rs.getString("address"),
            rs.getString("accent")
    );

    private final JdbcTemplate jdbc;

    public StoreDaoImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Store> selectAll() {
        return jdbc.query("""
                SELECT id, name, short_name, address, accent
                FROM stores
                ORDER BY id
                """, STORE_MAPPER);
    }

    @Override
    public boolean existsById(long id) {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM stores WHERE id = ?", Long.class, id);
        return count != null && count > 0;
    }

    @Override
    public List<StoreSalesDTO> selectStoreSales(OrderQueryDTO query) {
        List<Object> params = new ArrayList<>(OrderFilter.timeRangeParams(query));
        String storeFilter = "";
        if (query.storeId() != null) {
            storeFilter = "WHERE s.id = ?";
            params.add(query.storeId());
        }
        return jdbc.query("""
                SELECT
                  s.id,
                  s.name,
                  s.short_name,
                  s.address,
                  s.accent,
                  COUNT(o.id) AS orders,
                  COALESCE(ROUND(SUM(o.total_amount), 2), 0) AS revenue
                FROM stores s
                LEFT JOIN orders o
                  ON o.store_id = s.id
                  AND o.ordered_at >= ?
                  AND o.ordered_at < ?
                %s
                GROUP BY s.id, s.name, s.short_name, s.address, s.accent
                ORDER BY revenue DESC, s.id ASC
                """.formatted(storeFilter), (rs, rowNum) -> new StoreSalesDTO(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("short_name"),
                rs.getString("address"),
                rs.getString("accent"),
                rs.getLong("orders"),
                rs.getDouble("revenue")
        ), params.toArray());
    }

    @Override
    public void batchInsert(List<Store> stores) {
        jdbc.batchUpdate(
                "INSERT INTO stores (id, name, short_name, address, accent) VALUES (?, ?, ?, ?, ?)",
                stores,
                100,
                (statement, store) -> {
                    statement.setLong(1, store.getId());
                    statement.setString(2, store.getName());
                    statement.setString(3, store.getShortName());
                    statement.setString(4, store.getAddress());
                    statement.setString(5, store.getAccent());
                }
        );
    }

    @Override
    public void deleteAll() {
        jdbc.update("DELETE FROM stores");
    }
}
