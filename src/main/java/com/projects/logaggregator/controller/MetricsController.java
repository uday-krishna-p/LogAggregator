package com.projects.logaggregator.controller;

import com.projects.logaggregator.dto.AggregatedMetricsHistoryDto;
import com.projects.logaggregator.metrics.MetricsTracker;
import com.projects.logaggregator.model.MetricSnapshotEntity;
import com.projects.logaggregator.service.MetricsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/metrics")
public class MetricsController {

    private final MetricsService metricsService;

    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping("/raw-history")
    public List<MetricSnapshotEntity> getMetricSnapshots(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant to
    ) {

        return metricsService.getMetricSnapshots(from, to);
    }

    @GetMapping("history")
    public AggregatedMetricsHistoryDto getAggregatedMetricSnapshots(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant to,

            @RequestParam(defaultValue = "60") long bucketSizeSeconds
    ){
        return metricsService.getAggregatedMetrics(from, to, bucketSizeSeconds);
    }
}
