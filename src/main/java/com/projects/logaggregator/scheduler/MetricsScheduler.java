package com.projects.logaggregator.scheduler;

import com.projects.logaggregator.service.MetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MetricsScheduler {

    private final MetricsService metricsService;

    @Scheduled(fixedRate = 5000)
    public void collectMetrics() {
        metricsService.getMetricsData();
    }
}
