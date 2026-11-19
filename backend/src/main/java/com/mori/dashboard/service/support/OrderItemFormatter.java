package com.mori.dashboard.service.support;

import java.util.List;
import java.util.stream.Collectors;

import com.mori.dashboard.dto.stat.OrderItemLineDTO;

/**
 * 把订单商品行格式化为展示文本，例如 "云顶拿铁 ×2 · 黄油可颂 ×1"。
 */
public final class OrderItemFormatter {

    private OrderItemFormatter() {
    }

    public static String join(List<OrderItemLineDTO> items, String separator) {
        return items.stream()
                .map(item -> item.productName() + " ×" + item.quantity())
                .collect(Collectors.joining(separator));
    }
}
