package com.mori.dashboard.dao;

import java.util.List;

import com.mori.dashboard.dto.OrderQueryDTO;
import com.mori.dashboard.dto.stat.CategoryStatDTO;
import com.mori.dashboard.dto.stat.ProductSalesDTO;
import com.mori.dashboard.entity.OrderItem;

/**
 * 订单明细数据访问。
 */
public interface OrderItemDao {

    List<CategoryStatDTO> selectCategoryStats(OrderQueryDTO query);

    List<ProductSalesDTO> selectTopProducts(OrderQueryDTO query, int limit);

    void batchInsert(List<OrderItem> items);

    void deleteAll();
}
