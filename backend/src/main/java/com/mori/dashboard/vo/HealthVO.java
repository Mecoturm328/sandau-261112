package com.mori.dashboard.vo;

/**
 * 健康检查接口 /api/health 返回数据。
 */
public record HealthVO(String status, String database, long orders, PeriodVO dataRange) {
}
