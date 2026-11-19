package com.mori.dashboard.dao;

import java.util.List;

import com.mori.dashboard.entity.Product;

/**
 * 商品数据访问。
 */
public interface ProductDao {

    void batchInsert(List<Product> products);

    void deleteAll();
}
