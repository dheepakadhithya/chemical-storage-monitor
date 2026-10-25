package com.college.chemical_storage_monitor.controller;

import com.college.chemical_storage_monitor.model.Reading;
import com.college.chemical_storage_monitor.service.ReadingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class ReadingController {

    private final ReadingService readingService;

    @Autowired
    public ReadingController(ReadingService readingService) {
        this.readingService = readingService;
    }

    // 1. Store sensor data
    @PostMapping("/readings")
    public ResponseEntity<Reading> createReading(@RequestBody Reading reading) {
        if (reading.getCreatedAt() == null) {
            reading.setCreatedAt(java.time.LocalDateTime.now());
        }
        Reading processedReading = readingService.processNewReading(reading);
        return ResponseEntity.ok(processedReading);
    }

    // 2. Current values (supports optional chemicalId filter)
    @GetMapping("/readings/latest")
    public ResponseEntity<Reading> getLatestReading(@RequestParam(required = false) Long chemicalId) {
        Reading latest = readingService.getLatestReading(chemicalId);
        if (latest != null) {
            return ResponseEntity.ok(latest);
        } else {
            return ResponseEntity.noContent().build();
        }
    }

    // 3. Get all latest readings per chemical (for main dashboard overview cards)
    @GetMapping("/readings/all-latest")
    public ResponseEntity<Map<Long, Reading>> getAllLatestReadings() {
        return ResponseEntity.ok(readingService.getAllLatestReadings());
    }

    // 4. Alert history (supports optional chemicalId filter)
    @GetMapping("/alerts")
    public ResponseEntity<List<Reading>> getAlerts(@RequestParam(required = false) Long chemicalId) {
        List<Reading> alerts = readingService.getAlerts(chemicalId);
        return ResponseEntity.ok(alerts);
    }

    // 5. Full DB history (supports optional chemicalId filter)
    @GetMapping("/history")
    public ResponseEntity<List<Reading>> getFullHistory(@RequestParam(required = false) Long chemicalId) {
        List<Reading> history = readingService.getFullHistory(chemicalId);
        return ResponseEntity.ok(history);
    }
}
