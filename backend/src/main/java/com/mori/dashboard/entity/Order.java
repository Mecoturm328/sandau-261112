package com.mori.dashboard.entity;

import java.time.LocalDateTime;

/**
 * 订单表 orders
 */
public class Order {

    private Long id;
    private String orderNo;
    private Long storeId;
    private LocalDateTime orderedAt;
    private Double totalAmount;
    private Integer itemCount;
    private String paymentMethod;
    private boolean member;

    public Order() {
    }

    public Order(
            Long id,
            String orderNo,
            Long storeId,
            LocalDateTime orderedAt,
            Double totalAmount,
            Integer itemCount,
            String paymentMethod,
            boolean member
    ) {
        this.id = id;
        this.orderNo = orderNo;
        this.storeId = storeId;
        this.orderedAt = orderedAt;
        this.totalAmount = totalAmount;
        this.itemCount = itemCount;
        this.paymentMethod = paymentMethod;
        this.member = member;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public Long getStoreId() {
        return storeId;
    }

    public void setStoreId(Long storeId) {
        this.storeId = storeId;
    }

    public LocalDateTime getOrderedAt() {
        return orderedAt;
    }

    public void setOrderedAt(LocalDateTime orderedAt) {
        this.orderedAt = orderedAt;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Integer getItemCount() {
        return itemCount;
    }

    public void setItemCount(Integer itemCount) {
        this.itemCount = itemCount;
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public boolean isMember() {
        return member;
    }

    public void setMember(boolean member) {
        this.member = member;
    }
}
