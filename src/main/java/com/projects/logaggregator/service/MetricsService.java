package com.projects.logaggregator.service;

import com.projects.logaggregator.dto.AggregatedMetricsHistoryDto;
import com.projects.logaggregator.dto.MetricPointDto;
import com.projects.logaggregator.metrics.MetricsTracker;
import com.projects.logaggregator.model.MetricSnapshotEntity;
import com.projects.logaggregator.repository.MetricsSnapshotRepository;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.redis.connection.stream.StreamInfo.XInfoGroup;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MetricsService {

    private final MetricsTracker metricsTracker;
    private final MetricsSnapshotRepository metricsSnapshotRepository;

    private final String CONSUMER_GROUP = "log_group";
    private final String CONSUMER_NAME = "consumer-1";
    private final String STREAM_KEY;

    private final RedisTemplate<String, Object> redisTemplate;

    public MetricsService(MetricsTracker metricsTracker,
                          MetricsSnapshotRepository metricsSnapshotRepository,
                          @Value("${redis.stream.key}")String streamKey,
                          RedisTemplate<String, Object> redisTemplate) {
        this.metricsTracker = metricsTracker;
        this.metricsSnapshotRepository = metricsSnapshotRepository;
        STREAM_KEY = streamKey;
        this.redisTemplate = redisTemplate;
    }

    public void updateRedisLag(){
        StreamOperations<String, ? ,? > streamOperations = redisTemplate.opsForStream();
        List<XInfoGroup> groups = streamOperations.groups(STREAM_KEY).toList();

        for(XInfoGroup group : groups){
            if(CONSUMER_GROUP.equals(group.groupName())){
                Map<String, Object> raw = group.getRaw();
                Object lagObj = raw.get("lag");
                long lag = lagObj == null? 0L : Long.parseLong(lagObj.toString());
                metricsTracker.setPendingInRedisStream(lag);
                break;
            }
        }

    }
    public void getMetricsData() {

        long batches = metricsTracker.getTotalBatchesProcessed().get();
        long avgBatchTime = batches>0? metricsTracker.getTotalBatchTime().get()/batches :0 ;
        updateRedisLag();

        MetricSnapshotEntity snapshotEntity = MetricSnapshotEntity.builder()
                .capturedAt(Instant.now())
                .generatedLogsCount(metricsTracker.getGeneratedLogs().get())
                .processedLogsCount(metricsTracker.getProcessedLogs().get())
                .failedLogsCount(metricsTracker.getFailedLogs().get())
                .pendingInRedisStream(metricsTracker.getPendingInRedisStream())
                .avgBatchTimeMs(avgBatchTime)
                .totalBatchesProcessed(batches)
                .build();

        metricsSnapshotRepository.save(snapshotEntity);
        metricsTracker.reset();
        System.out.println("Added MetricsSnapshot to DB");
    }

    public List<MetricSnapshotEntity> getMetricSnapshots(Instant from, Instant to) {
        if(from != null && to != null){
            return metricsSnapshotRepository.findByCapturedAtBetweenOrderByCapturedAtAsc(from, to);
        }
        return metricsSnapshotRepository.findAll();
    }

    public AggregatedMetricsHistoryDto getAggregatedMetrics(Instant from, Instant to, long bucketSize) {
//        String bucket = resolveTruncUnit(bucketSize);
        List<Object[]> results = metricsSnapshotRepository.getGroupedMetrics(from, to, bucketSize);
        List<MetricPointDto> redisLag = new ArrayList<>();
        List<MetricPointDto> processed = new ArrayList<>();
        List<MetricPointDto> failed = new ArrayList<>();
        List<MetricPointDto> generated = new ArrayList<>();
        List<MetricPointDto> avgBatchTimeMs = new ArrayList<>();
        List<MetricPointDto> batchesProcessed = new ArrayList<>();

        for(Object[] row : results){
            Instant time = (Instant) row[0];
            redisLag.add(new MetricPointDto(time, getLong(row[1])));
            processed.add(new MetricPointDto(time, getLong(row[2])));
            failed.add(new MetricPointDto(time, getLong(row[3])));
            generated.add(new MetricPointDto(time, getLong(row[4])));
            avgBatchTimeMs.add(new MetricPointDto(time, getLong(row[5])));
            batchesProcessed.add(new MetricPointDto(time, getLong(row[6])));
        }
        return new AggregatedMetricsHistoryDto(redisLag, processed, failed, generated, avgBatchTimeMs, batchesProcessed);
    }

    private Instant convertToInstant(Object timestampObj) {
        if (timestampObj instanceof Timestamp ts) {
            return ts.toInstant();
        } else if (timestampObj instanceof LocalDateTime ldt) {
            return ldt.atZone(ZoneId.systemDefault()).toInstant(); // or ZoneOffset.UTC
        } else {
            throw new IllegalArgumentException("Unsupported timestamp type: " + timestampObj.getClass());
        }
    }
    private String resolveTruncUnit(Duration bucketSize){
        long sec = bucketSize.getSeconds();
        if(sec <= 10) return "second";
        if(sec <= 60) return "minute";
        if(sec <= 300) return "5 minutes";
        if(sec <= 3600) return "hour";
        return "day";
    }

    private long getLong(Object value) {
        return value == null ? 0 : ((Number) value).longValue();
    }

}
