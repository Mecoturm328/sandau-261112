package com.mori.dashboard.dao.impl;

import java.util.List;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import com.mori.dashboard.dao.ProductDao;
import com.mori.dashboard.entity.Product;

@Repository
public class ProductDaoImpl implements ProductDao {

    private final JdbcTemplate jdbc;

    public ProductDaoImpl(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void batchInsert(List<Product> products) {
        jdbc.batchUpdate(
                "INSERT INTO products (id, name, category, price, sort_order) VALUES (?, ?, ?, ?, ?)",
                products,
                100,
                (statement, product) -> {
                    statement.setLong(1, product.getId());
                    statement.setString(2, product.getName());
                    statement.setString(3, product.getCategory());
                    statement.setDouble(4, product.getPrice());
                    statement.setInt(5, product.getSortOrder());
                }
        );
    }

    @Override
    public void deleteAll() {
        jdbc.update("DELETE FROM products");
    }
}
