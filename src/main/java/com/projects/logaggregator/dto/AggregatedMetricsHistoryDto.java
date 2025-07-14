package com.projects.logaggregator.dto;

import java.util.List;

public record AggregatedMetricsHistoryDto(
        List<MetricPointDto> redisLag,
        List<MetricPointDto> processedLogs,
        List<MetricPointDto> failedLogs,
        List<MetricPointDto> generatedLogs,
        List<MetricPointDto> avgBatchTimeMs,
        List<MetricPointDto> totalBatchesProcessed
) {
}
