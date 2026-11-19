package com.mori.dashboard.service.impl;

import org.springframework.stereotype.Service;

import com.mori.dashboard.service.HealthService;
import com.mori.dashboard.service.support.DataOverview;
import com.mori.dashboard.service.support.OrderQueryResolver;
import com.mori.dashboard.vo.HealthVO;
import com.mori.dashboard.vo.PeriodVO;

@Service
public class HealthServiceImpl implements HealthService {

    private final OrderQueryResolver queryResolver;

    public HealthServiceImpl(OrderQueryResolver queryResolver) {
        this.queryResolver = queryResolver;
    }

    @Override
    public HealthVO getHealth() {
        DataOverview overview = queryResolver.loadOverview();
        return new HealthVO(
                "ok",
                "sqlite",
                overview.totalOrders(),
                new PeriodVO(overview.minDate().toString(), overview.maxDate().toString())
        );
    }
}
