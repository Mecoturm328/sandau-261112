package com.mori.dashboard.vo;

/**
 * 导出文件（文件名 + 内容），由 Controller 写入 HTTP 响应。
 */
public record ExportFileVO(String filename, String contentType, byte[] content) {
}
