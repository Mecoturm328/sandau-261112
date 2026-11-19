package com.mori.dashboard.service;

public interface DemoDataService {

    /**
     * 订单表为空时生成演示数据。
     *
     * @return 新生成的订单数；已有数据时返回 0
     */
    int initIfEmpty();
}
