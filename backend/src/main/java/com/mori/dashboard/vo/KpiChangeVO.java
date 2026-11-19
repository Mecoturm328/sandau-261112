package com.mori.dashboard.vo;

/**
 * 环比变化（百分比；上一周期无数据时为 null）。
 */
public record KpiChangeVO(Double revenue, Double orders, Double avgTicket, Double memberShare) {
}
