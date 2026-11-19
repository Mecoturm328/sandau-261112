package com.mori.dashboard.dao.impl;

import java.util.ArrayList;
import java.util.List;

import com.mori.dashboard.dto.OrderQueryDTO;

/**
 * 把查询条件转换为 orders 表（别名 o）的 WHERE 子句与参数，仅供 DAO 实现内部使用。
 */
final class OrderFilter {

    private final String clause;
    private final List<Object> params;

    private OrderFilter(String clause, List<Object> params) {
        this.clause = clause;
        this.params = params;
    }

    static OrderFilter of(OrderQueryDTO query) {
        StringBuilder clause = new StringBuilder("o.ordered_at >= ? AND o.ordered_at < ?");
        List<Object> params = new ArrayList<>(timeRangeParams(query));
        if (query.storeId() != null) {
            clause.append(" AND o.store_id = ?");
            params.add(query.storeId());
        }
        return new OrderFilter(clause.toString(), params);
    }

    /** ordered_at 以 TEXT "yyyy-MM-dd HH:mm:ss" 存储，按左闭右开区间比较。 */
    static List<Object> timeRangeParams(OrderQueryDTO query) {
        return List.of(
                query.start() + " 00:00:00",
                query.end().plusDays(1) + " 00:00:00"
        );
    }

    String clause() {
        return clause;
    }

    Object[] params() {
        return params.toArray();
    }

    Object[] paramsWith(Object... extra) {
        List<Object> all = new ArrayList<>(params);
        all.addAll(List.of(extra));
        return all.toArray();
    }
}
