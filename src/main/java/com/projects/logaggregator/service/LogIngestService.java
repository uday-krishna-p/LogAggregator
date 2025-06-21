package com.projects.logaggregator.service;

import com.projects.logaggregator.metrics.MetricsTracker;
import com.projects.logaggregator.model.LogEntryDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class LogIngestService {

    private final String STREAM_KEY;
    private final RedisTemplate<String, Object> redisTemplate;
    private final MetricsTracker metricsTracker;

    public LogIngestService(RedisTemplate<String, Object> redisTemplate, @Value("${redis.stream.key}")String streamKey, MetricsTracker metricsTracker) {

        this.STREAM_KEY = streamKey;
        this.redisTemplate = redisTemplate;
        this.metricsTracker = metricsTracker;
    }

    public void pushToStream(LogEntryDTO logEntryDTO) {
        Map<String, Object> fields = new HashMap<>();
        fields.put("timestamp", logEntryDTO.getTimestamp());
        fields.put("message", logEntryDTO.getMessage());
        fields.put("level", logEntryDTO.getLevel());
        fields.put("source", logEntryDTO.getSource() != null ? logEntryDTO.getSource() : "unknown");
        fields.put("traceId", logEntryDTO.getTraceId() != null ? logEntryDTO.getTraceId() : "");

        RecordId recordId = redisTemplate.opsForStream().add(StreamRecords.mapBacked(fields).withStreamKey(STREAM_KEY));
        metricsTracker.incrementLogsIngestedToRedis();
        System.out.println("Pushed to Redis Stream with ID: " + recordId);
    }
}
