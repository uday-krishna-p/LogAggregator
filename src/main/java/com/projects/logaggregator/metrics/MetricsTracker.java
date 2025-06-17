package com.projects.logaggregator.metrics;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Getter
@Setter
@Component
public class MetricsTracker {
    private final AtomicLong totalLogsProcessedToDB = new AtomicLong();
    private final AtomicLong totalBatchesProcessedToDB = new AtomicLong();
    private final AtomicLong totalIngestedToRedis = new AtomicLong();
    private final AtomicLong totalProcessingTimeToDB = new AtomicLong();
    private volatile long lastProcessingTime = 0;
    private volatile int lastBatchSize = 0;
    private volatile long logsUnreadInRedisStream = 0;

    public void logIngestedToRedis() {
        totalIngestedToRedis.incrementAndGet();
    }
    public void logBatchProcessedToDB(int batchSize, long timeTakenMs) {
        totalLogsProcessedToDB.addAndGet(batchSize);
        totalBatchesProcessedToDB.incrementAndGet();
        totalProcessingTimeToDB.addAndGet(timeTakenMs);
        lastBatchSize = batchSize;
        lastProcessingTime = timeTakenMs;
    }

    public double getAverageDBProcessingLatencyMs(){
        long count = totalLogsProcessedToDB.get();
        return count == 0 ? 0 : (double)totalProcessingTimeToDB.get() / count;
    }

}
