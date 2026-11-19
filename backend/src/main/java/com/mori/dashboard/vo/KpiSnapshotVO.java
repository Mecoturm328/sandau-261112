package com.mori.dashboard.vo;

/**
 * 上一周期核心指标。
 */
public record KpiSnapshotVO(double revenue, long orders, double avgTicket, double memberShare) {
}
