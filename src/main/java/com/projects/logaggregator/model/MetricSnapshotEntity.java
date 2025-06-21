package com.projects.logaggregator.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name="metrics_snapshot")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetricSnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Instant capturedAt;

    private long generatedLogsCount;
    private long processedLogsCount;
    private long failedLogsCount;

    private long pendingInRedisStream;

    private long avgBatchTimeMs;
    private long totalBatchesProcessed;

}
