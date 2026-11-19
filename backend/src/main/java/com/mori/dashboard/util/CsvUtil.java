package com.mori.dashboard.util;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

import com.mori.dashboard.dto.DashboardResponse.OrderExportRow;

public final class CsvUtil {

    private CsvUtil() {
    }

    public static byte[] toCsv(List<OrderExportRow> rows) {
        StringBuilder builder = new StringBuilder("\uFEFF");
        builder.append(String.join(",", List.of(
                cell("订单号"),
                cell("下单时间"),
                cell("门店"),
                cell("商品明细"),
                cell("件数"),
                cell("金额（元）"),
                cell("支付方式"),
                cell("会员订单")
        ))).append("\r\n");

        for (OrderExportRow row : rows) {
            builder.append(String.join(",", List.of(
                    cell(row.orderNo()),
                    cell(row.orderedAt()),
                    cell(row.store()),
                    cell(row.items()),
                    cell(Long.toString(row.itemCount())),
                    cell(String.format(Locale.ROOT, "%.2f", row.total())),
                    cell(row.payment()),
                    cell(row.isMember() ? "是" : "否")
            ))).append("\r\n");
        }

        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static String cell(String value) {
        String text = value == null ? "" : value;
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
}
