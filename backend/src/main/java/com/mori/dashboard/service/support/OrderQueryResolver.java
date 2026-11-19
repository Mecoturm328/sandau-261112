package com.mori.dashboard.service.support;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

import org.springframework.stereotype.Component;

import com.mori.dashboard.dao.OrderDao;
import com.mori.dashboard.dao.StoreDao;
import com.mori.dashboard.dto.DashboardQueryDTO;
import com.mori.dashboard.dto.OrderQueryDTO;
import com.mori.dashboard.dto.stat.OrderOverviewDTO;
import com.mori.dashboard.exception.BusinessException;
import com.mori.dashboard.util.NumberUtil;

/**
 * Service 层公共组件：读取数据集概览，并把前端原始参数校验、解析为 DAO 可用的查询条件。
 */
@Component
public class OrderQueryResolver {

    public static final int DEFAULT_DAYS = 30;
    public static final int MAX_RANGE_DAYS = 366;

    private static final String DATE_PATTERN = "\\d{4}-\\d{2}-\\d{2}";

    private final OrderDao orderDao;
    private final StoreDao storeDao;

    public OrderQueryResolver(OrderDao orderDao, StoreDao storeDao) {
        this.orderDao = orderDao;
        this.storeDao = storeDao;
    }

    /** 数据集概览；库中没有订单时日期范围退化为今天。 */
    public DataOverview loadOverview() {
        OrderOverviewDTO overview = orderDao.selectOverview();
        LocalDate today = LocalDate.now();
        return new DataOverview(
                overview.minDate() == null ? today : LocalDate.parse(overview.minDate()),
                overview.maxDate() == null ? today : LocalDate.parse(overview.maxDate()),
                overview.totalOrders(),
                NumberUtil.round(overview.totalRevenue(), 2)
        );
    }

    /**
     * 校验并解析查询参数：
     * <ul>
     *   <li>开始、结束日期需同时提供，格式 YYYY-MM-DD，且开始不晚于结束；</li>
     *   <li>都不提供时默认取数据集最后 {@value #DEFAULT_DAYS} 天；</li>
     *   <li>单次最多 {@value #MAX_RANGE_DAYS} 天；</li>
     *   <li>门店 ID 必须为存在的门店。</li>
     * </ul>
     */
    public OrderQueryDTO resolve(DashboardQueryDTO query, DataOverview overview) {
        Long storeId = resolveStore(query.store());

        boolean hasStart = hasText(query.start());
        boolean hasEnd = hasText(query.end());
        if (hasStart != hasEnd) {
            throw new BusinessException("开始日期与结束日期需要同时提供。");
        }

        LocalDate start;
        LocalDate end;
        if (hasStart) {
            start = parseDate(query.start(), "开始日期");
            end = parseDate(query.end(), "结束日期");
        } else {
            end = overview.maxDate();
            LocalDate defaultStart = end.minusDays(DEFAULT_DAYS - 1L);
            start = defaultStart.isBefore(overview.minDate()) ? overview.minDate() : defaultStart;
        }

        if (start.isAfter(end)) {
            throw new BusinessException("开始日期不能晚于结束日期。");
        }
        if (ChronoUnit.DAYS.between(start, end) + 1 > MAX_RANGE_DAYS) {
            throw new BusinessException("单次查询最多支持 " + MAX_RANGE_DAYS + " 天。");
        }
        return new OrderQueryDTO(start, end, storeId);
    }

    private Long resolveStore(String store) {
        if (!hasText(store)) {
            return null;
        }
        long storeId;
        try {
            if (!store.matches("\\d+")) {
                throw new NumberFormatException(store);
            }
            storeId = Long.parseLong(store);
        } catch (NumberFormatException exception) {
            throw new BusinessException("门店参数无效。");
        }
        if (!storeDao.existsById(storeId)) {
            throw new BusinessException("所选门店不存在。");
        }
        return storeId;
    }

    private static LocalDate parseDate(String value, String fieldName) {
        if (!value.matches(DATE_PATTERN)) {
            throw new BusinessException(fieldName + "格式应为 YYYY-MM-DD。");
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw new BusinessException(fieldName + "不是有效日期。");
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
