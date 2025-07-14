package com.projects.logaggregator.repository;

import com.projects.logaggregator.model.MetricSnapshotEntity;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface MetricsSnapshotRepository extends JpaRepository<MetricSnapshotEntity, Long> {
    List<MetricSnapshotEntity> findByCapturedAtBetweenOrderByCapturedAtAsc(Instant capturedAtAfter, Instant capturedAtBefore);

    @Query(value = """
        SELECT 
                 to_timestamp(floor(EXTRACT(EPOCH FROM captured_at) / :bucket) * :bucket) AS bucket_time,
               SUM(pending_in_redis_stream),
               SUM(processed_logs_count),
               SUM(failed_logs_count),
               SUM(generated_logs_count),
               AVG(avg_batch_time_ms),
               SUM(total_batches_processed)
        FROM metrics_snapshot
        WHERE captured_at BETWEEN :from AND :to
        GROUP BY bucket_time
        ORDER BY bucket_time
        """, nativeQuery = true)
    List<Object[]> getGroupedMetrics(@Param("from") Instant from,
                                     @Param("to")  Instant to,
                                     @Param("bucket") long bucket
    );

}
