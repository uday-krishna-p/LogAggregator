package com.projects.logaggregator.service;

import com.projects.logaggregator.metrics.MetricsTracker;
import com.projects.logaggregator.model.LogStorageEntryEntity;
import com.projects.logaggregator.repository.LogStorageEntryRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class LogStreamConsumer {
    private final String CONSUMER_GROUP = "log_group";
    private final String CONSUMER_NAME = "consumer-1";

    private final String STREAM_KEY;
    private final RedisTemplate<String, Object> redisTemplate;

    private final MetricsTracker metricsTracker;
    private final LogStorageEntryRepository logRepo;

    public volatile boolean running = true;

    public LogStreamConsumer(RedisTemplate<String, Object> redisTemplate, @Value("${redis.stream.key}")String streamKey, MetricsTracker metricsTracker, LogStorageEntryRepository logRepo) {

        this.STREAM_KEY = streamKey;
        this.redisTemplate = redisTemplate;
        this.metricsTracker = metricsTracker;
        this.logRepo = logRepo;
    }


    @PostConstruct
    @EventListener(ApplicationReadyEvent.class)
    public void startConsumer() {
        try{
            redisTemplate.opsForStream().createGroup(STREAM_KEY, ReadOffset.from("0"), CONSUMER_GROUP);
            System.out.println("Consumer group created");
        } catch(Exception e){
            System.out.println("Consumer group creation failed" + e.getMessage());
        }

        new Thread(() -> {
            while(running) {
                pollStream();
            }
        }).start();
    }

    public void pollStream() {
            try{
                long startTime = System.currentTimeMillis();
                List<MapRecord<String, Object, Object>> messages = redisTemplate.opsForStream().read(
                        Consumer.from(CONSUMER_GROUP, CONSUMER_NAME),
                        StreamReadOptions.empty().count(10).block(Duration.ofSeconds(5)),
                        StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed())
                );
                if(messages != null && !messages.isEmpty()){
                    boolean success = pushToDB(messages);
                    long endTime = System.currentTimeMillis();
                    long timeTaken = endTime - startTime;
                    metricsTracker.logBatchProcessedToDB(success?messages.size():0, timeTaken);
                    Thread.sleep(200);
                }
            }catch (Exception e){
                System.out.println("Error while reading from Redis Stream" + e.getMessage());
            }
    }

    @PreDestroy
    public void onShutdown() {
        running = false;
        System.out.println("Consumer group stopped");
    }

    private boolean pushToDB(List<MapRecord<String, Object, Object>> messages){
        boolean success = true;
        System.out.println("Consumed Logs from Redis Stream of batch size : " + messages.size() + "\n Now pushing to Postgres DB!");
        List<LogStorageEntryEntity> allLogs = new ArrayList<>();
        for(MapRecord<String, Object, Object> message : messages){
            Map<Object, Object> fields = message.getValue();
            LogStorageEntryEntity entity = LogStorageEntryEntity.builder().timestamp(fields.get("timestamp").toString())
                    .level(fields.get("level").toString()).message(fields.get("message").toString())
                    .message(fields.get("message").toString())
                    .source(fields.get("source").toString())
                    .traceId(fields.get("traceId").toString())
                    .build();
            allLogs.add(entity);
//            System.out.println("Pushed log from Redis to Postgres: " + entity);
            redisTemplate.opsForStream().acknowledge(STREAM_KEY, CONSUMER_GROUP, message.getId());
        }
        try{
            logRepo.saveAll(allLogs);
        }
        catch(Exception e){
            metricsTracker.incrementLogsFailedToDB(messages.size());
            success = false;
            System.out.println("Error while saving Logs from Redis Stream" + e.getMessage());
        }
        return success;
    }
}
