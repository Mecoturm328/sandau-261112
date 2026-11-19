package com.mori.dashboard.service;

import com.mori.dashboard.dto.DashboardQueryDTO;
import com.mori.dashboard.vo.DashboardVO;

public interface DashboardService {

    /**
     * 查询看板聚合数据，并与上一等长周期对比。
     *
     * @throws com.mori.dashboard.exception.BusinessException 查询参数不合法时
     */
    DashboardVO getDashboard(DashboardQueryDTO query);
}
