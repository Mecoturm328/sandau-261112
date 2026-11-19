package com.mori.dashboard.dto;

/**
 * 前端传入的原始查询参数（未校验）。
 *
 * @param start 开始日期 YYYY-MM-DD，可为空
 * @param end   结束日期 YYYY-MM-DD，可为空
 * @param store 门店 ID，可为空表示全部门店
 */
public record DashboardQueryDTO(String start, String end, String store) {
}
