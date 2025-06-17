package com.projects.logaggregator.service;

import com.projects.logaggregator.model.LogEntryDTO;
import com.projects.logaggregator.model.LogStorageEntryEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

@Service
public class LogService {

    private final LogIngestService logIngestService;

    @PersistenceContext
    private EntityManager entityManager;

    public LogService(LogIngestService logIngestService) {
        this.logIngestService = logIngestService;
    }


    public Page<LogStorageEntryEntity> filterLogs(String level, String startTime, String endTime, String searchTerm, Pageable pageable) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<LogStorageEntryEntity> cq = cb.createQuery(LogStorageEntryEntity.class);
        Root<LogStorageEntryEntity> root = cq.from(LogStorageEntryEntity.class);

        List<Predicate> predicates = new ArrayList<>();

        if(level != null && !level.isBlank()) {
            predicates.add(cb.equal(root.get("level"), level));
        }
        if(startTime != null && !startTime.isBlank()) {
            predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), startTime));
        }
        if(endTime != null && !endTime.isBlank()) {
            predicates.add(cb.lessThanOrEqualTo(root.get("timestamp"), endTime));
        }

        if(searchTerm != null && !searchTerm.isBlank()) {
            predicates.add(cb.like(cb.lower(root.get("message")), "%" + searchTerm.toLowerCase() + "%"));
        }


        cq.where(predicates.toArray(new Predicate[0]));

        if(pageable.getSort().isSorted()) {
            pageable.getSort().forEach(sort -> {
                Path<Object> path = root.get(sort.getProperty());
                cq.orderBy(sort.isAscending()?cb.asc(path):cb.desc(path));
            });
        }

        List<LogStorageEntryEntity> results = entityManager.createQuery(cq).setFirstResult((int) pageable.getOffset())
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<LogStorageEntryEntity> countRoot = countQuery.from(LogStorageEntryEntity.class);
        countQuery.select(cb.count(countRoot));
        countQuery.where(predicates.toArray(new Predicate[0]));
        Long count = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(results, pageable, count);
    }

    public void generateRandomLogs(int count, int delayMs) {
        for(int i = 0; i < count; i++) {
            LogEntryDTO dto = new LogEntryDTO();
            dto.setLevel(generateRandomLevel());
            dto.setMessage(generateRandomMessage());
            dto.setTimestamp(Instant.now().toString());
            dto.setSource(generateRandomSource());
            dto.setTraceId(UUID.randomUUID().toString());
            logIngestService.pushToStream(dto);
            if(delayMs > 0) {
                try{
                    Thread.sleep(delayMs);
                }
                catch(InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted white generating logs ", e);
                }
            }
        }
    }

    private String generateRandomSource(){
        return List.of("service-1", "service-2", "service-3", "service-4", "service-5").get(new Random().nextInt(5));
    }

    private String generateRandomLevel(){
        return List.of("INFO", "DEBUG", "WARN", "ERROR").get(new Random().nextInt(4));
    }

    private String generateRandomMessage() {
        List<String> templates = List.of(
                "User login attempt failed for userId: %s",
                "Processed order with ID: %s successfully",
                "Payment gateway responded with status: %s",
                "Error parsing request payload for endpoint: %s",
                "TraceId %s reached final stage",
                "Cache miss for key: %s",
                "Rate limit exceeded for IP: %s",
                "Database timeout while querying: %s"
        );

        String randomValue = UUID.randomUUID().toString().substring(0, 8);
        String template = templates.get(new Random().nextInt(templates.size()));
        return String.format(template, randomValue);
    }

}
