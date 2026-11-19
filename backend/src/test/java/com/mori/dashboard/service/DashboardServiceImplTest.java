package com.mori.dashboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.mori.dashboard.dao.OrderDao;
import com.mori.dashboard.dao.OrderItemDao;
import com.mori.dashboard.dao.StoreDao;
import com.mori.dashboard.dto.DashboardQueryDTO;
import com.mori.dashboard.dto.OrderQueryDTO;
import com.mori.dashboard.dto.stat.CategoryStatDTO;
import com.mori.dashboard.dto.stat.DailyStatDTO;
import com.mori.dashboard.dto.stat.HourlyStatDTO;
import com.mori.dashboard.dto.stat.KpiStatDTO;
import com.mori.dashboard.dto.stat.OrderDetailDTO;
import com.mori.dashboard.dto.stat.OrderItemLineDTO;
import com.mori.dashboard.dto.stat.OrderOverviewDTO;
import com.mori.dashboard.service.impl.DashboardServiceImpl;
import com.mori.dashboard.service.support.OrderQueryResolver;
import com.mori.dashboard.vo.DashboardVO;

class DashboardServiceImplTest {

    private final OrderDao orderDao = mock(OrderDao.class);
    private final OrderItemDao orderItemDao = mock(OrderItemDao.class);
    private final StoreDao storeDao = mock(StoreDao.class);
    private final DashboardService service = new DashboardServiceImpl(
            new OrderQueryResolver(orderDao, storeDao), orderDao, orderItemDao, storeDao);

    @Test
    void computesKpisFillsGapsAndFormatsOrders() {
        OrderQueryDTO current = new OrderQueryDTO(LocalDate.parse("2026-11-01"), LocalDate.parse("2026-11-03"), null);
        OrderQueryDTO previous = new OrderQueryDTO(LocalDate.parse("2026-10-29"), LocalDate.parse("2026-10-31"), null);

        when(orderDao.selectOverview()).thenReturn(new OrderOverviewDTO("2026-10-01", "2026-11-03", 50, 1500));
        when(orderDao.selectKpiStat(current)).thenReturn(new KpiStatDTO(300, 8, 12, 3));
        when(orderDao.selectKpiStat(previous)).thenReturn(new KpiStatDTO(200, 0, 0, 0));
        when(orderDao.selectDailyStats(current)).thenReturn(List.of(new DailyStatDTO("2026-11-02", 300, 8, 3)));
        when(orderDao.selectHourlyStats(current)).thenReturn(List.of(new HourlyStatDTO(9, 8, 300)));
        when(orderItemDao.selectCategoryStats(current)).thenReturn(List.of(
                new CategoryStatDTO("咖啡", 200, 7), new CategoryStatDTO("烘焙", 100, 5)));
        when(orderItemDao.selectTopProducts(eq(current), anyInt())).thenReturn(List.of());
        when(storeDao.selectStoreSales(any())).thenReturn(List.of());
        when(storeDao.selectAll()).thenReturn(List.of());
        when(orderDao.selectOrderDetails(eq(current), eq(7))).thenReturn(List.of(new OrderDetailDTO(
                "MR-1", "2026-11-02 09:12:00", "梧桐里店", "梧桐里", "#d97745", 54, 3, "微信支付", true,
                List.of(new OrderItemLineDTO("云顶拿铁", 2), new OrderItemLineDTO("黄油可颂", 1)))));

        DashboardVO vo = service.getDashboard(new DashboardQueryDTO("2026-11-01", "2026-11-03", null));

        assertEquals(3, vo.range().days());
        assertEquals("2026-10-29", vo.range().previous().start());
        assertEquals(37.5, vo.kpis().avgTicket());
        assertEquals(37.5, vo.kpis().memberShare());
        assertEquals(50.0, vo.kpis().change().revenue());
        assertNull(vo.kpis().change().orders());
        assertNull(vo.kpis().change().memberShare());

        assertEquals(3, vo.trend().size());
        assertEquals(0, vo.trend().get(0).orders());
        assertEquals(8, vo.trend().get(1).orders());

        assertEquals(13, vo.hourly().size());
        assertEquals("08:00", vo.hourly().get(0).label());
        assertEquals(8, vo.hourly().get(1).orders());

        assertEquals(66.7, vo.categories().get(0).share());
        assertEquals("云顶拿铁 ×2 · 黄油可颂 ×1", vo.recentOrders().get(0).items());
    }
}
