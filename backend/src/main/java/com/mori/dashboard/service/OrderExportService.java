package com.mori.dashboard.service;

import com.mori.dashboard.dto.DashboardQueryDTO;
import com.mori.dashboard.vo.ExportFileVO;

public interface OrderExportService {

    /**
     * 按筛选条件导出订单 CSV。
     *
     * @throws com.mori.dashboard.exception.BusinessException 查询参数不合法时
     */
    ExportFileVO exportOrders(DashboardQueryDTO query);
}
