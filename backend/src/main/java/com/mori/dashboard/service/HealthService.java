package com.mori.dashboard.service;

import com.mori.dashboard.vo.HealthVO;

public interface HealthService {

    /** 服务状态、订单总量与数据日期范围。 */
    HealthVO getHealth();
}
