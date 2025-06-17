package com.projects.logaggregator.controller;

import com.projects.logaggregator.metrics.MetricsTracker;
import com.projects.logaggregator.service.MetricsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/metrics")
public class MetricsController {

    private final MetricsService metricsService;

    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping
    public Map<String, Object> getMetrics() {
        return metricsService.getMetricsData();
    }
}
