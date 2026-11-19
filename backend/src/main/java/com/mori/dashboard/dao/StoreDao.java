package com.mori.dashboard.dao;

import java.util.List;

import com.mori.dashboard.dto.OrderQueryDTO;
import com.mori.dashboard.dto.stat.StoreSalesDTO;
import com.mori.dashboard.entity.Store;

/**
 * 门店数据访问。
 */
public interface StoreDao {

    List<Store> selectAll();

    boolean existsById(long id);

    /**
     * 按门店统计查询区间内的订单量与营业额（没有订单的门店也返回 0）。
     */
    List<StoreSalesDTO> selectStoreSales(OrderQueryDTO query);

    void batchInsert(List<Store> stores);

    void deleteAll();
}
