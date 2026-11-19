package com.mori.dashboard.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.mori.dashboard.service.DemoDataService;

/**
 * 应用启动后检查数据库，订单为空时调用 Service 生成演示数据。
 */
@Component
public class DemoDataInitializer implements ApplicationRunner {

    private final DemoDataService demoDataService;
    private final boolean seedEnabled;

    public DemoDataInitializer(
            DemoDataService demoDataService,
            @Value("${app.seed.enabled:true}") boolean seedEnabled
    ) {
        this.demoDataService = demoDataService;
        this.seedEnabled = seedEnabled;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (seedEnabled) {
            demoDataService.initIfEmpty();
        }
    }
}
