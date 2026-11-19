package com.mori.dashboard.service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.stereotype.Service;

import com.mori.dashboard.dto.DashboardResponse;
import com.mori.dashboard.dto.DashboardResponse.Changes;
import com.mori.dashboard.dto.DashboardResponse.Kpis;
import com.mori.dashboard.dto.DashboardResponse.Meta;
import com.mori.dashboard.dto.DashboardResponse.OrderExportRow;
import com.mori.dashboard.dto.DashboardResponse.PreviousKpis;
import com.mori.dashboard.dto.DashboardResponse.PreviousPeriod;
import com.mori.dashboard.dto.DashboardResponse.RangeInfo;
import com.mori.dashboard.exception.BadRequestException;
import com.mori.dashboard.repository.DashboardRepository;
import com.mori.dashboard.repository.DashboardRepository.Bounds;
import com.mori.dashboard.repository.DashboardRepository.KpiTotals;

@Service
public class DashboardService {

    private static final int DEFAULT_DAYS = 30;
    private static final int MAX_RANGE_DAYS = 366;

    private final DashboardRepository repository;

    public DashboardService(DashboardRepository repository) {
        this.repository = repository;
    }

    public record ResolvedQuery(
            LocalDate start,
            LocalDate end,
            Long storeId,
            Bounds bounds
    ) {
    }

    public record ExportResult(
            ResolvedQuery query,
            List<OrderExportRow> rows
    ) {
    }

    public ResolvedQuery resolveQuery(String startParam, String endParam, String storeParam) {
        Bounds bounds = repository.getBounds();
        Long storeId = resolveStore(storeParam);

        LocalDate start;
        LocalDate end;

        boolean hasStart = startParam != null && !startParam.isBlank();
        boolean hasEnd = endParam != null && !endParam.isBlank();
        if (hasStart != hasEnd) {
            throw new BadRequestException("开始日期与结束日期需要同时提供。");
        }

        if (hasStart) {
            start = parseDate(startParam, "开始日期");
            end = parseDate(endParam, "结束日期");
        } else {
            end = parseDate(bounds.maxDate(), "数据结束日期");
            LocalDate thirtyDaysAgo = end.minusDays(DEFAULT_DAYS - 1L);
            LocalDate minDate = parseDate(bounds.minDate(), "数据开始日期");
            start = thirtyDaysAgo.isBefore(minDate) ? minDate : thirtyDaysAgo;
        }

        if (start.isAfter(end)) {
            throw new BadRequestException("开始日期不能晚于结束日期。");
        }
        long days = ChronoUnit.DAYS.between(start, end) + 1;
        if (days > MAX_RANGE_DAYS) {
            throw new BadRequestException("单次查询最多支持 " + MAX_RANGE_DAYS + " 天。");
        }

        return new ResolvedQuery(start, end, storeId, bounds);
    }

    public DashboardResponse getDashboard(String startParam, String endParam, String storeParam) {
        ResolvedQuery query = resolveQuery(startParam, endParam, storeParam);
        int days = Math.toIntExact(ChronoUnit.DAYS.between(query.start(), query.end()) + 1);
        LocalDate previousStart = query.start().minusDays(days);
        LocalDate previousEnd = query.start().minusDays(1);

        KpiTotals current = repository.getKpis(query.start(), query.end(), query.storeId());
        KpiTotals previous = repository.getKpis(previousStart, previousEnd, query.storeId());

        double currentAvgTicket = current.orders() == 0
                ? 0
                : round(current.revenue() / current.orders(), 2);
        double currentMemberShare = current.orders() == 0
                ? 0
                : round((double) current.memberOrders() / current.orders() * 100, 1);
        double previousAvgTicket = previous.orders() == 0
                ? 0
                : round(previous.revenue() / previous.orders(), 2);
        double previousMemberShare = previous.orders() == 0
                ? 0
                : round((double) previous.memberOrders() / previous.orders() * 100, 1);

        PreviousKpis previousKpis = new PreviousKpis(
                previous.revenue(),
                previous.orders(),
                previousAvgTicket,
                previousMemberShare
        );

        Kpis kpis = new Kpis(
                current.revenue(),
                current.orders(),
                current.items(),
                currentAvgTicket,
                currentMemberShare,
                current.memberOrders(),
                previousKpis,
                new Changes(
                        percentChange(current.revenue(), previous.revenue()),
                        percentChange(current.orders(), previous.orders()),
                        percentChange(currentAvgTicket, previousAvgTicket),
                        previous.orders() == 0
                                ? null
                                : round(currentMemberShare - previousMemberShare, 1)
                )
        );

        return new DashboardResponse(
                new Meta(
                        OffsetDateTime.now().toString(),
                        "SQLite",
                        query.bounds().totalOrders(),
                        query.bounds().totalRevenue()
                ),
                new RangeInfo(
                        query.start().toString(),
                        query.end().toString(),
                        days,
                        query.bounds().minDate(),
                        query.bounds().maxDate(),
                        new PreviousPeriod(previousStart.toString(), previousEnd.toString())
                ),
                query.storeId(),
                repository.getStores(),
                kpis,
                repository.getTrend(query.start(), query.end(), query.storeId()),
                repository.getCategories(query.start(), query.end(), query.storeId()),
                repository.getHourly(query.start(), query.end(), query.storeId()),
                repository.getTopProducts(query.start(), query.end(), query.storeId()),
                repository.getStorePerformance(query.start(), query.end(), query.storeId()),
                repository.getRecentOrders(query.start(), query.end(), query.storeId())
        );
    }

    public ExportResult getExportRows(String startParam, String endParam, String storeParam) {
        ResolvedQuery query = resolveQuery(startParam, endParam, storeParam);
        return new ExportResult(query, repository.getExportRows(query.start(), query.end(), query.storeId()));
    }

    public Bounds getBounds() {
        return repository.getBounds();
    }

    private Long resolveStore(String storeParam) {
        if (storeParam == null || storeParam.isBlank()) {
            return null;
        }
        if (!storeParam.matches("\\d+")) {
            throw new BadRequestException("门店参数无效。");
        }
        long storeId;
        try {
            storeId = Long.parseLong(storeParam);
        } catch (NumberFormatException exception) {
            throw new BadRequestException("门店参数无效。");
        }
        if (!repository.storeExists(storeId)) {
            throw new BadRequestException("所选门店不存在。");
        }
        return storeId;
    }

    private LocalDate parseDate(String value, String fieldName) {
        if (value == null || !value.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new BadRequestException(fieldName + "格式应为 YYYY-MM-DD。");
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            throw new BadRequestException(fieldName + "不是有效日期。");
        }
    }

    private Double percentChange(double current, double previous) {
        if (previous == 0) {
            return null;
        }
        return round((current - previous) / previous * 100, 1);
    }

    private static double round(double value, int digits) {
        double factor = Math.pow(10, digits);
        return Math.round((value + 1e-9) * factor) / factor;
    }
}
