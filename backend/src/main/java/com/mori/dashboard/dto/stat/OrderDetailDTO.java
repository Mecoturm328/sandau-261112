package com.mori.dashboard.dto.stat;

import java.util.List;

/**
 * 订单详情（含门店信息与商品行）。
 */
public record OrderDetailDTO(
        String orderNo,
        String orderedAt,
        String storeName,
        String storeShortName,
        String storeAccent,
        double totalAmount,
        long itemCount,
        String paymentMethod,
        boolean member,
        List<OrderItemLineDTO> items
) {
}
