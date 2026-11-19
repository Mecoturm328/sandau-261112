package com.mori.dashboard.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 全链路测试：Controller → Service → DAO → 临时 SQLite（启动时自动生成演示数据）。
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest {

    @TempDir
    static Path tempDir;

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + tempDir.resolve("test.db"));
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthReportsSeededData() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.status").value("ok"))
                .andExpect(jsonPath("$.data.orders").value(greaterThan(10000)));
    }

    @Test
    void dashboardReturnsDefaultRange() throws Exception {
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.data.range.days").value(30))
                .andExpect(jsonPath("$.data.stores", hasSize(3)))
                .andExpect(jsonPath("$.data.trend", hasSize(30)))
                .andExpect(jsonPath("$.data.hourly", hasSize(13)))
                .andExpect(jsonPath("$.data.topProducts", hasSize(6)))
                .andExpect(jsonPath("$.data.recentOrders", hasSize(7)))
                .andExpect(jsonPath("$.data.recentOrders[0].isMember").isBoolean());
    }

    @Test
    void dashboardFiltersByStore() throws Exception {
        mockMvc.perform(get("/api/dashboard").param("store", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.storeId").value(2))
                .andExpect(jsonPath("$.data.storePerformance", hasSize(1)))
                .andExpect(jsonPath("$.data.recentOrders[0].store").value("苏河"));
    }

    @Test
    void invalidParametersReturn400Result() throws Exception {
        mockMvc.perform(get("/api/dashboard").param("start", "2026-10-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").value("开始日期与结束日期需要同时提供。"))
                .andExpect(jsonPath("$.data").doesNotExist());

        mockMvc.perform(get("/api/dashboard").param("store", "99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("所选门店不存在。"));
    }

    @Test
    void exportReturnsCsvAttachment() throws Exception {
        mockMvc.perform(get("/api/orders/export"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", startsWith("text/csv")))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andExpect(header().string("Content-Disposition", containsString("mori-orders-")))
                .andExpect(content().string(startsWith("﻿\"订单号\"")));
    }

    @Test
    void unknownPathReturns404Result() throws Exception {
        mockMvc.perform(get("/api/nope"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }
}
