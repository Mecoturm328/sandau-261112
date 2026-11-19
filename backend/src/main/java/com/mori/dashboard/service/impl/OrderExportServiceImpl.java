package com.mori.dashboard.service.impl;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import com.mori.dashboard.dao.OrderDao;
import com.mori.dashboard.dto.DashboardQueryDTO;
import com.mori.dashboard.dto.OrderQueryDTO;
import com.mori.dashboard.dto.stat.OrderDetailDTO;
import com.mori.dashboard.service.OrderExportService;
import com.mori.dashboard.service.support.OrderItemFormatter;
import com.mori.dashboard.service.support.OrderQueryResolver;
import com.mori.dashboard.util.CsvUtil;
import com.mori.dashboard.util.NumberUtil;
import com.mori.dashboard.vo.ExportFileVO;

@Service
public class OrderExportServiceImpl implements OrderExportService {

    private static final String CONTENT_TYPE = "text/csv; charset=UTF-8";
    private static final String ITEM_SEPARATOR = " / ";
    private static final List<String> HEADERS = List.of(
            "订单号", "下单时间", "门店", "商品明细", "件数", "金额（元）", "支付方式", "会员订单"
    );

    private final OrderQueryResolver queryResolver;
    private final OrderDao orderDao;

    public OrderExportServiceImpl(OrderQueryResolver queryResolver, OrderDao orderDao) {
        this.queryResolver = queryResolver;
        this.orderDao = orderDao;
    }

    @Override
    public ExportFileVO exportOrders(DashboardQueryDTO params) {
        OrderQueryDTO query = queryResolver.resolve(params, queryResolver.loadOverview());
        List<List<String>> rows = orderDao.selectOrderDetails(query, null).stream()
                .map(OrderExportServiceImpl::toRow)
                .toList();

        String filename = "mori-orders-" + query.start() + "-" + query.end() + ".csv";
        return new ExportFileVO(filename, CONTENT_TYPE, CsvUtil.write(HEADERS, rows));
    }

    private static List<String> toRow(OrderDetailDTO order) {
        return List.of(
                order.orderNo(),
                order.orderedAt(),
                order.storeName(),
                OrderItemFormatter.join(order.items(), ITEM_SEPARATOR),
                Long.toString(order.itemCount()),
                String.format(Locale.ROOT, "%.2f", NumberUtil.round(order.totalAmount(), 2)),
                order.paymentMethod(),
                order.member() ? "是" : "否"
        );
    }
}
