package com.projects.logaggregator.repository;

import com.projects.logaggregator.model.MetricSnapshotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface MetricsSnapshotRepository extends JpaRepository<MetricSnapshotEntity, Long> {
    List<MetricSnapshotEntity> findByCapturedAtBetweenOrderByCapturedAtAsc(Instant capturedAtAfter, Instant capturedAtBefore);
}
