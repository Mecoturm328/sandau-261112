package com.mori.dashboard.dto.stat;

/**
 * 订单中的一行商品。
 */
public record OrderItemLineDTO(String productName, int quantity) {
}
