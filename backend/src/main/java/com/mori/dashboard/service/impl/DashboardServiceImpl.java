package com.mori.dashboard.service.impl;

import static com.mori.dashboard.util.NumberUtil.divide;
import static com.mori.dashboard.util.NumberUtil.percent;
import static com.mori.dashboard.util.NumberUtil.percentChange;
import static com.mori.dashboard.util.NumberUtil.round;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.mori.dashboard.dao.OrderDao;
import com.mori.dashboard.dao.OrderItemDao;
import com.mori.dashboard.dao.StoreDao;
import com.mori.dashboard.dto.DashboardQueryDTO;
import com.mori.dashboard.dto.OrderQueryDTO;
import com.mori.dashboard.dto.stat.CategoryStatDTO;
import com.mori.dashboard.dto.stat.DailyStatDTO;
import com.mori.dashboard.dto.stat.HourlyStatDTO;
import com.mori.dashboard.dto.stat.KpiStatDTO;
import com.mori.dashboard.service.DashboardService;
import com.mori.dashboard.service.support.DataOverview;
import com.mori.dashboard.service.support.OrderItemFormatter;
import com.mori.dashboard.service.support.OrderQueryResolver;
import com.mori.dashboard.vo.CategorySliceVO;
import com.mori.dashboard.vo.DashboardVO;
import com.mori.dashboard.vo.HourlyPointVO;
import com.mori.dashboard.vo.KpiChangeVO;
import com.mori.dashboard.vo.KpiSnapshotVO;
import com.mori.dashboard.vo.KpiVO;
import com.mori.dashboard.vo.MetaVO;
import com.mori.dashboard.vo.PeriodVO;
import com.mori.dashboard.vo.RangeVO;
import com.mori.dashboard.vo.RecentOrderVO;
import com.mori.dashboard.vo.StorePerformanceVO;
import com.mori.dashboard.vo.StoreVO;
import com.mori.dashboard.vo.TopProductVO;
import com.mori.dashboard.vo.TrendPointVO;

@Service
public class DashboardServiceImpl implements DashboardService {

    private static final String DATA_SOURCE = "SQLite";
    private static final int TOP_PRODUCT_LIMIT = 6;
    private static final int RECENT_ORDER_LIMIT = 7;
    private static final int FIRST_BUSINESS_HOUR = 8;
    private static final int LAST_BUSINESS_HOUR = 20;
    private static final String ITEM_SEPARATOR = " · ";

    private final OrderQueryResolver queryResolver;
    private final OrderDao orderDao;
    private final OrderItemDao orderItemDao;
    private final StoreDao storeDao;

    public DashboardServiceImpl(
            OrderQueryResolver queryResolver,
            OrderDao orderDao,
            OrderItemDao orderItemDao,
            StoreDao storeDao
    ) {
        this.queryResolver = queryResolver;
        this.orderDao = orderDao;
        this.orderItemDao = orderItemDao;
        this.storeDao = storeDao;
    }

    @Override
    public DashboardVO getDashboard(DashboardQueryDTO params) {
        DataOverview overview = queryResolver.loadOverview();
        OrderQueryDTO query = queryResolver.resolve(params, overview);

        int days = Math.toIntExact(ChronoUnit.DAYS.between(query.start(), query.end()) + 1);
        OrderQueryDTO previousQuery = new OrderQueryDTO(
                query.start().minusDays(days),
                query.start().minusDays(1),
                query.storeId()
        );

        return new DashboardVO(
                new MetaVO(
                        OffsetDateTime.now().toString(),
                        DATA_SOURCE,
                        overview.totalOrders(),
                        overview.totalRevenue()
                ),
                new RangeVO(
                        query.start().toString(),
                        query.end().toString(),
                        days,
                        overview.minDate().toString(),
                        overview.maxDate().toString(),
                        new PeriodVO(previousQuery.start().toString(), previousQuery.end().toString())
                ),
                query.storeId(),
                listStores(),
                buildKpis(orderDao.selectKpiStat(query), orderDao.selectKpiStat(previousQuery)),
                buildTrend(query),
                buildCategories(query),
                buildHourly(query),
                buildTopProducts(query),
                buildStorePerformance(query),
                buildRecentOrders(query)
        );
    }

    private List<StoreVO> listStores() {
        return storeDao.selectAll().stream()
                .map(store -> new StoreVO(
                        store.getId(),
                        store.getName(),
                        store.getShortName(),
                        store.getAddress(),
                        store.getAccent()
                ))
                .toList();
    }

    private KpiVO buildKpis(KpiStatDTO current, KpiStatDTO previous) {
        double revenue = round(current.revenue(), 2);
        double previousRevenue = round(previous.revenue(), 2);
        double avgTicket = divide(current.revenue(), current.orders(), 2);
        double previousAvgTicket = divide(previous.revenue(), previous.orders(), 2);
        double memberShare = percent(current.memberOrders(), current.orders(), 1);
        double previousMemberShare = percent(previous.memberOrders(), previous.orders(), 1);

        return new KpiVO(
                revenue,
                current.orders(),
                current.items(),
                avgTicket,
                memberShare,
                current.memberOrders(),
                new KpiSnapshotVO(previousRevenue, previous.orders(), previousAvgTicket, previousMemberShare),
                new KpiChangeVO(
                        percentChange(revenue, previousRevenue),
                        percentChange(current.orders(), previous.orders()),
                        percentChange(avgTicket, previousAvgTicket),
                        previous.orders() == 0 ? null : round(memberShare - previousMemberShare, 1)
                )
        );
    }

    /** 每日营收趋势；没有订单的日期补 0，保证折线连续。 */
    private List<TrendPointVO> buildTrend(OrderQueryDTO query) {
        Map<String, DailyStatDTO> byDate = orderDao.selectDailyStats(query).stream()
                .collect(Collectors.toMap(DailyStatDTO::date, Function.identity()));

        List<TrendPointVO> trend = new ArrayList<>();
        for (LocalDate date = query.start(); !date.isAfter(query.end()); date = date.plusDays(1)) {
            DailyStatDTO stat = byDate.get(date.toString());
            trend.add(stat == null
                    ? new TrendPointVO(date.toString(), 0, 0, 0)
                    : new TrendPointVO(stat.date(), round(stat.revenue(), 2), stat.orders(), stat.memberOrders()));
        }
        return trend;
    }

    /** 品类构成，附带营收占比（百分比，1 位小数）。 */
    private List<CategorySliceVO> buildCategories(OrderQueryDTO query) {
        List<CategoryStatDTO> stats = orderItemDao.selectCategoryStats(query);
        double total = stats.stream().mapToDouble(CategoryStatDTO::revenue).sum();
        return stats.stream()
                .map(stat -> new CategorySliceVO(
                        stat.category(),
                        round(stat.revenue(), 2),
                        stat.quantity(),
                        percent(stat.revenue(), total, 1)
                ))
                .toList();
    }

    /** 营业时段（8:00–20:00）订单分布，无订单的小时补 0。 */
    private List<HourlyPointVO> buildHourly(OrderQueryDTO query) {
        Map<Integer, HourlyStatDTO> byHour = orderDao.selectHourlyStats(query).stream()
                .collect(Collectors.toMap(HourlyStatDTO::hour, Function.identity()));

        List<HourlyPointVO> hourly = new ArrayList<>();
        for (int hour = FIRST_BUSINESS_HOUR; hour <= LAST_BUSINESS_HOUR; hour += 1) {
            HourlyStatDTO stat = byHour.get(hour);
            String label = String.format(Locale.ROOT, "%02d:00", hour);
            hourly.add(stat == null
                    ? new HourlyPointVO(hour, label, 0, 0)
                    : new HourlyPointVO(hour, label, stat.orders(), round(stat.revenue(), 2)));
        }
        return hourly;
    }

    private List<TopProductVO> buildTopProducts(OrderQueryDTO query) {
        return orderItemDao.selectTopProducts(query, TOP_PRODUCT_LIMIT).stream()
                .map(stat -> new TopProductVO(stat.name(), stat.category(), stat.quantity(), round(stat.revenue(), 2)))
                .toList();
    }

    private List<StorePerformanceVO> buildStorePerformance(OrderQueryDTO query) {
        return storeDao.selectStoreSales(query).stream()
                .map(stat -> new StorePerformanceVO(
                        stat.storeId(),
                        stat.name(),
                        stat.shortName(),
                        stat.address(),
                        stat.accent(),
                        stat.orders(),
                        round(stat.revenue(), 2),
                        divide(stat.revenue(), stat.orders(), 2)
                ))
                .toList();
    }

    private List<RecentOrderVO> buildRecentOrders(OrderQueryDTO query) {
        return orderDao.selectOrderDetails(query, RECENT_ORDER_LIMIT).stream()
                .map(order -> new RecentOrderVO(
                        order.orderNo(),
                        order.orderedAt(),
                        order.storeShortName(),
                        order.storeAccent(),
                        round(order.totalAmount(), 2),
                        order.itemCount(),
                        order.paymentMethod(),
                        order.member(),
                        OrderItemFormatter.join(order.items(), ITEM_SEPARATOR)
                ))
                .toList();
    }
}
