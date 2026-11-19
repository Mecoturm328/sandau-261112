package com.mori.dashboard.dto;

import java.time.LocalDate;

/**
 * 经过 Service 层校验后、传给 DAO 层的订单查询条件。
 *
 * @param start   开始日期（包含）
 * @param end     结束日期（包含）
 * @param storeId 门店 ID，null 表示全部门店
 */
public record OrderQueryDTO(LocalDate start, LocalDate end, Long storeId) {
}
