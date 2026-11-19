package com.mori.dashboard.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class CsvUtilTest {

    @Test
    void writesBomHeaderAndCrlf() {
        String csv = new String(CsvUtil.write(
                List.of("订单号", "门店"),
                List.of(List.of("MR-W-20261111-018", "梧桐里店"))
        ), StandardCharsets.UTF_8);

        assertEquals("﻿\"订单号\",\"门店\"\r\n\"MR-W-20261111-018\",\"梧桐里店\"\r\n", csv);
    }

    @Test
    void doublesQuotesAndHandlesNull() {
        String csv = new String(CsvUtil.write(
                List.of("门店", "备注"),
                List.of(Arrays.asList("门店 \"A\"", null))
        ), StandardCharsets.UTF_8);

        assertTrue(csv.contains("\"门店 \"\"A\"\"\",\"\""));
    }
}
