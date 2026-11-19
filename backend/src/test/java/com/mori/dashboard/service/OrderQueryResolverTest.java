package com.mori.dashboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mori.dashboard.dao.OrderDao;
import com.mori.dashboard.dao.StoreDao;
import com.mori.dashboard.dto.DashboardQueryDTO;
import com.mori.dashboard.dto.OrderQueryDTO;
import com.mori.dashboard.exception.BusinessException;
import com.mori.dashboard.service.support.DataOverview;
import com.mori.dashboard.service.support.OrderQueryResolver;

class OrderQueryResolverTest {

    private final DataOverview overview = new DataOverview(
            LocalDate.parse("2026-07-15"), LocalDate.parse("2026-11-11"), 100, 1000);
    private OrderQueryResolver resolver;

    @BeforeEach
    void setUp() {
        StoreDao storeDao = mock(StoreDao.class);
        when(storeDao.existsById(2L)).thenReturn(true);
        resolver = new OrderQueryResolver(mock(OrderDao.class), storeDao);
    }

    @Test
    void defaultsToLastThirtyDaysOfDataset() {
        OrderQueryDTO query = resolver.resolve(new DashboardQueryDTO(null, null, null), overview);
        assertEquals(LocalDate.parse("2026-10-13"), query.start());
        assertEquals(LocalDate.parse("2026-11-11"), query.end());
        assertNull(query.storeId());
    }

    @Test
    void acceptsExplicitRangeAndStore() {
        OrderQueryDTO query = resolver.resolve(new DashboardQueryDTO("2026-10-01", "2026-10-31", "2"), overview);
        assertEquals(new OrderQueryDTO(LocalDate.parse("2026-10-01"), LocalDate.parse("2026-10-31"), 2L), query);
    }

    @Test
    void rejectsInvalidParameters() {
        assertMessage("开始日期与结束日期需要同时提供。", new DashboardQueryDTO("2026-10-01", null, null));
        assertMessage("开始日期格式应为 YYYY-MM-DD。", new DashboardQueryDTO("2026/10/01", "2026-10-02", null));
        assertMessage("开始日期不是有效日期。", new DashboardQueryDTO("2026-13-01", "2026-10-02", null));
        assertMessage("开始日期不能晚于结束日期。", new DashboardQueryDTO("2026-10-05", "2026-10-02", null));
        assertMessage("单次查询最多支持 366 天。", new DashboardQueryDTO("2025-01-01", "2026-10-02", null));
        assertMessage("门店参数无效。", new DashboardQueryDTO(null, null, "abc"));
        assertMessage("门店参数无效。", new DashboardQueryDTO(null, null, "99999999999999999999"));
        assertMessage("所选门店不存在。", new DashboardQueryDTO(null, null, "9"));
    }

    private void assertMessage(String expected, DashboardQueryDTO params) {
        BusinessException exception = assertThrows(BusinessException.class, () -> resolver.resolve(params, overview));
        assertEquals(expected, exception.getMessage());
    }
}
