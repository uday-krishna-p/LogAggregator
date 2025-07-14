package com.projects.logaggregator.dto;

import java.time.Instant;

public record MetricPointDto(Instant timestamp, long value) {
}
