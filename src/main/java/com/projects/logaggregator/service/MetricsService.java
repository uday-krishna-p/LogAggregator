package com.projects.logaggregator.service;

import com.projects.logaggregator.metrics.MetricsTracker;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.redis.connection.stream.StreamInfo.XInfoGroup;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MetricsService {

    private final MetricsTracker metricsTracker;

    private final String CONSUMER_GROUP = "log_group";
    private final String CONSUMER_NAME = "consumer-1";

    private final String STREAM_KEY;

    private final RedisTemplate<String, Object> redisTemplate;

    public MetricsService(MetricsTracker metricsTracker, @Value("${redis.stream.key}")String streamKey, RedisTemplate<String, Object> redisTemplate) {
        this.metricsTracker = metricsTracker;
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
                metricsTracker.setLogsUnreadInRedisStream(lag);
                break;
            }
        }

    }
    public Map<String, Object> getMetricsData() {

        Map<String, Object> metrics = new LinkedHashMap<>();
        updateRedisLag();
        metrics.put("totalLogsProcessed", metricsTracker.getTotalLogsProcessedToDB());
        metrics.put("totalBatchesProcessed", metricsTracker.getTotalBatchesProcessedToDB());
        metrics.put("totalIngestedToRedis", metricsTracker.getTotalIngestedToRedis());
        metrics.put("averageProcessingLatencyMs", metricsTracker.getAverageDBProcessingLatencyMs());
        metrics.put("lastBatchSize", metricsTracker.getLastBatchSize());
        metrics.put("lastProcessingTimeMs", metricsTracker.getLastProcessingTime());
        metrics.put("logsInRedisStreamPending", metricsTracker.getLogsUnreadInRedisStream());
        return metrics;
    }

}
