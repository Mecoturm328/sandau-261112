package com.mori.dashboard.util;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.mori.dashboard.dto.DashboardResponse.OrderExportRow;

class CsvUtilTest {

    @Test
    void writesBomHeaderAndEscapesValues() {
        byte[] bytes = CsvUtil.toCsv(List.of(
                new OrderExportRow(
                        "MR-W-20261111-018",
                        "2026-11-11 20:18:38",
                        "梧桐里店",
                        "拿铁 ×1 · 可颂 ×2",
                        3,
                        54.0,
                        "微信支付",
                        true
                )
        ));

        String csv = new String(bytes, StandardCharsets.UTF_8);
        assertTrue(csv.startsWith("\uFEFF\"订单号\""));
        assertTrue(csv.contains("\"梧桐里店\""));
        assertTrue(csv.contains("\"54.00\""));
        assertTrue(csv.contains("\"是\""));
        assertTrue(csv.endsWith("\r\n"));
    }

    @Test
    void doublesQuotesInsideCells() {
        byte[] bytes = CsvUtil.toCsv(List.of(
                new OrderExportRow(
                        "MR-1", "2026-11-11 10:00:00", "门店 \"A\"", "拿铁", 1, 32.0, "支付宝", false
                )
        ));
        String csv = new String(bytes, StandardCharsets.UTF_8);
        assertTrue(csv.contains("\"门店 \"\"A\"\"\""));
    }
}
