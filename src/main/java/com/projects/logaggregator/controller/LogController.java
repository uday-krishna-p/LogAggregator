package com.projects.logaggregator.controller;

import com.projects.logaggregator.model.LogEntryDTO;
import com.projects.logaggregator.model.LogStorageEntryEntity;
import com.projects.logaggregator.service.LogIngestService;
import com.projects.logaggregator.service.LogService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/logs")
public class LogController {

    @Autowired
    private LogIngestService logIngestService;
    @Autowired
    private LogService logService;

    @PostMapping
    public ResponseEntity<Void> ingestLog(@Valid @RequestBody LogEntryDTO logEntryDTO) {
        logIngestService.pushToStream(logEntryDTO);
        return ResponseEntity.accepted().build();
    }

    @PostMapping("/generate")
    public ResponseEntity<String> generateLogs(@RequestParam int count, @RequestParam(required = false, defaultValue = "0") int delayMs){
        logService.generateRandomLogs(count, delayMs);
        return ResponseEntity.ok("Generated " + count +" Random Logs with delay " + delayMs + " milliseconds");
    }

    @GetMapping
    public Page<LogStorageEntryEntity> getLogs(@RequestParam(required = false) String level,
                                               @RequestParam(required = false) String startTime,
                                               @RequestParam(required = false)String endTime,
                                               @RequestParam(required = false)String searchTerm,
                                               @PageableDefault(size = 20, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable) {

        return logService.filterLogs(level, startTime, endTime, searchTerm, pageable);

    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Log Aggregator is running great!");
    }
}
