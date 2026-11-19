package com.mori.dashboard.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mori.dashboard.common.Result;
import com.mori.dashboard.service.HealthService;
import com.mori.dashboard.vo.HealthVO;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping("/health")
    public Result<HealthVO> health() {
        return Result.success(healthService.getHealth());
    }
}
