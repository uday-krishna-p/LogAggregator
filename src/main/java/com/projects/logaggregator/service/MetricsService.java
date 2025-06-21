package com.projects.logaggregator.service;

import com.projects.logaggregator.metrics.MetricsTracker;
import com.projects.logaggregator.model.MetricSnapshotEntity;
import com.projects.logaggregator.repository.MetricsSnapshotRepository;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.redis.connection.stream.StreamInfo.XInfoGroup;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.stereotype.Service;

import java.time.Instant;
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

}
