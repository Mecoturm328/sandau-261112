package com.mori.dashboard.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mori.dashboard.dto.DashboardResponse;
import com.mori.dashboard.repository.DashboardRepository.Bounds;
import com.mori.dashboard.service.DashboardService;
import com.mori.dashboard.service.DashboardService.ExportResult;
import com.mori.dashboard.service.DashboardService.ResolvedQuery;
import com.mori.dashboard.util.CsvUtil;

import jakarta.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("/api")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Bounds bounds = dashboardService.getBounds();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("status", "ok");
        response.put("database", "sqlite");
        response.put("orders", bounds.totalOrders());
        response.put("dataRange", Map.of(
                "start", bounds.minDate(),
                "end", bounds.maxDate()
        ));
        return response;
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard(
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) String store,
            HttpServletResponse response
    ) {
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        return dashboardService.getDashboard(start, end, store);
    }

    @GetMapping("/orders/export")
    public ResponseEntity<byte[]> exportOrders(
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) String store
    ) {
        ExportResult export = dashboardService.getExportRows(start, end, store);
        ResolvedQuery query = export.query();
        byte[] body = CsvUtil.toCsv(export.rows());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv; charset=UTF-8"));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("mori-orders-" + query.start() + "-" + query.end() + ".csv")
                .build());
        headers.setCacheControl("no-store");

        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(body.length)
                .body(body);
    }
}
