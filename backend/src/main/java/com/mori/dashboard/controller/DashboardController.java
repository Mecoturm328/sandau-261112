package com.mori.dashboard.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mori.dashboard.common.Result;
import com.mori.dashboard.dto.DashboardQueryDTO;
import com.mori.dashboard.service.DashboardService;
import com.mori.dashboard.vo.DashboardVO;

@RestController
@RequestMapping("/api")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * 看板聚合数据。参数：start、end（YYYY-MM-DD，需同时提供）、store（门店 ID）。
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Result<DashboardVO>> dashboard(DashboardQueryDTO query) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(Result.success(dashboardService.getDashboard(query)));
    }
}
