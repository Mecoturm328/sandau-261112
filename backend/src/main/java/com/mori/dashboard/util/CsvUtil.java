package com.mori.dashboard.util;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 通用 CSV 生成：UTF-8 + BOM（Excel 可直接打开），每个单元格加双引号，CRLF 换行。
 */
public final class CsvUtil {

    private static final String BOM = "﻿";
    private static final String LINE_END = "\r\n";

    private CsvUtil() {
    }

    public static byte[] write(List<String> headers, List<List<String>> rows) {
        StringBuilder builder = new StringBuilder(BOM);
        appendLine(builder, headers);
        for (List<String> row : rows) {
            appendLine(builder, row);
        }
        return builder.toString().getBytes(StandardCharsets.UTF_8);
    }

    private static void appendLine(StringBuilder builder, List<String> cells) {
        builder.append(cells.stream().map(CsvUtil::cell).collect(Collectors.joining(","))).append(LINE_END);
    }

    private static String cell(String value) {
        String text = value == null ? "" : value;
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }
}
