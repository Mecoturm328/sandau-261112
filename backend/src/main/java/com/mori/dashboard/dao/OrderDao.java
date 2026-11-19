package com.mori.dashboard.dao;

import java.util.List;

import com.mori.dashboard.dto.OrderQueryDTO;
import com.mori.dashboard.dto.stat.DailyStatDTO;
import com.mori.dashboard.dto.stat.HourlyStatDTO;
import com.mori.dashboard.dto.stat.KpiStatDTO;
import com.mori.dashboard.dto.stat.OrderDetailDTO;
import com.mori.dashboard.dto.stat.OrderOverviewDTO;
import com.mori.dashboard.entity.Order;

/**
 * 订单数据访问。
 */
public interface OrderDao {

    long count();

    OrderOverviewDTO selectOverview();

    KpiStatDTO selectKpiStat(OrderQueryDTO query);

    List<DailyStatDTO> selectDailyStats(OrderQueryDTO query);

    List<HourlyStatDTO> selectHourlyStats(OrderQueryDTO query);

    /**
     * 按下单时间倒序查询订单详情。
     *
     * @param limit 最多返回条数；null 表示不限制
     */
    List<OrderDetailDTO> selectOrderDetails(OrderQueryDTO query, Integer limit);

    void batchInsert(List<Order> orders);

    void deleteAll();
}
