package com.mori.dashboard.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mori.dashboard.dto.DashboardQueryDTO;
import com.mori.dashboard.service.OrderExportService;
import com.mori.dashboard.vo.ExportFileVO;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderExportService orderExportService;

    public OrderController(OrderExportService orderExportService) {
        this.orderExportService = orderExportService;
    }

    /**
     * 按筛选条件导出订单 CSV，参数同 /api/dashboard。
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(DashboardQueryDTO query) {
        ExportFileVO file = orderExportService.exportOrders(query);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(file.filename())
                        .build()
                        .toString())
                .cacheControl(CacheControl.noStore())
                .contentLength(file.content().length)
                .body(file.content());
    }
}
