package com.projects.logaggregator.metrics;

import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Getter
@Setter
@Component
public class MetricsTracker {


    private final AtomicLong generatedLogs = new AtomicLong(0);
    private final AtomicLong processedLogs = new AtomicLong(0);
    private final AtomicLong failedLogs = new AtomicLong(0);
    private final AtomicLong totalBatchesProcessed = new AtomicLong(0);
    private final AtomicLong totalBatchTime = new AtomicLong();
//    private final AtomicLong totalLogsInBatches = new AtomicLong(0);
    private long pendingInRedisStream = 0;

    public void incrementLogsIngestedToRedis() {
        generatedLogs.incrementAndGet();
    }

    public void incrementLogsFailedToDB(int count) {
        failedLogs.addAndGet(count);
    }
    public void logBatchProcessedToDB(int batchSize, long timeTakenMs) {
        totalBatchesProcessed.incrementAndGet();
        totalBatchTime.addAndGet(timeTakenMs);
        processedLogs.addAndGet(batchSize);
    }

    public void reset(){
        generatedLogs.set(0);
        processedLogs.set(0);
        failedLogs.set(0);
        totalBatchesProcessed.set(0);
        totalBatchTime.set(0);
        processedLogs.set(0);
        pendingInRedisStream = 0;

    }

//    public double getAverageDBProcessingLatencyMs(){
//        long count = totalLogsProcessedToDB.get();
//        return count == 0 ? 0 : (double)totalProcessingTimeToDB.get() / count;
//    }

}
