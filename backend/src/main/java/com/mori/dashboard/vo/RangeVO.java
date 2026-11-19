package com.mori.dashboard.vo;

/**
 * 本次查询的日期区间及数据集边界。
 */
public record RangeVO(String start, String end, int days, String minDate, String maxDate, PeriodVO previous) {
}
