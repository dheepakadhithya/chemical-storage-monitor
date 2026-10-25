package com.college.chemical_storage_monitor.controller;

import com.college.chemical_storage_monitor.service.DeviceControlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/controls")
@CrossOrigin(origins = "*")
public class DeviceControlController {

    private final DeviceControlService deviceControlService;

    @Autowired
    public DeviceControlController(DeviceControlService deviceControlService) {
        this.deviceControlService = deviceControlService;
    }

    // 1. Get all actuator states (ESP32 and frontend call this)
    @GetMapping
    public ResponseEntity<Map<String, Object>> getControlStates() {
        Map<String, Object> response = new HashMap<>();
        response.put("modes", deviceControlService.getAllModes());
        response.put("fan", deviceControlService.getMode("fan"));
        response.put("buzzer", deviceControlService.getMode("buzzer"));
        response.put("light", deviceControlService.getMode("light"));
        response.put("lightBrightness", deviceControlService.getLightBrightness());
        response.put("buzzerVolume", deviceControlService.getBuzzerVolume());
        response.put("buzzerPattern", deviceControlService.getBuzzerPattern());
        response.put("buzzerTone", deviceControlService.getBuzzerTone());
        response.put("cabinetLock", deviceControlService.getCabinetLock());
        response.put("distanceThreshold", deviceControlService.getDistanceThreshold());
        return ResponseEntity.ok(response);
    }

    // 2. Set actuator mode: NORMAL (auto), STOPPED (force off), ON (force on)
    @PostMapping("/{device}")
    public ResponseEntity<Map<String, Object>> setDeviceMode(
            @PathVariable String device,
            @RequestParam(required = false, defaultValue = "NORMAL") String mode) {

        boolean success = deviceControlService.setMode(device, mode);
        Map<String, Object> response = new HashMap<>();

        if (success) {
            response.put("success", true);
            response.put("device", device.toLowerCase());
            response.put("mode", mode.toUpperCase());
            response.put("allModes", deviceControlService.getAllModes());
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("error", "Invalid device or mode. Devices: fan, buzzer, light. Modes: NORMAL, STOPPED, ON");
            return ResponseEntity.badRequest().body(response);
        }
    }

    // 3. Set Light Brightness (PWM Dimming 0-100%)
    @PostMapping("/light/brightness")
    public ResponseEntity<Map<String, Object>> setLightBrightness(
            @RequestParam(required = false, defaultValue = "100") Integer value) {

        deviceControlService.setLightBrightness(value);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("device", "light");
        response.put("lightBrightness", deviceControlService.getLightBrightness());
        return ResponseEntity.ok(response);
    }

    // 4. Set Buzzer Pattern (CONTINUOUS, URGENT, PULSE, CHIRP)
    @PostMapping("/buzzer/pattern")
    public ResponseEntity<Map<String, Object>> setBuzzerPattern(
            @RequestParam(required = false, defaultValue = "CONTINUOUS") String pattern) {

        deviceControlService.setBuzzerPattern(pattern);
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("buzzerPattern", deviceControlService.getBuzzerPattern());
        return ResponseEntity.ok(response);
    }

    // 5. Set Buzzer Volume (0-100%)
    @PostMapping("/buzzer/volume")
    public ResponseEntity<Map<String, Object>> setBuzzerVolume(
            @RequestParam(required = false, defaultValue = "100") Integer value) {

        deviceControlService.setBuzzerVolume(value);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("buzzerVolume", deviceControlService.getBuzzerVolume());
        return ResponseEntity.ok(response);
    }

    // 6. Set Buzzer Tone Frequency (500-3500 Hz)
    @PostMapping("/buzzer/tone")
    public ResponseEntity<Map<String, Object>> setBuzzerTone(
            @RequestParam(required = false, defaultValue = "2000") Integer value) {

        deviceControlService.setBuzzerTone(value);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("buzzerTone", deviceControlService.getBuzzerTone());
        return ResponseEntity.ok(response);
    }

    // 7. Cabinet Security Lock Mode (LOCKED vs UNLOCKED)
    @PostMapping("/lock")
    public ResponseEntity<Map<String, Object>> setCabinetLock(
            @RequestParam(required = false, defaultValue = "LOCKED") String state) {

        deviceControlService.setCabinetLock(state);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("cabinetLock", deviceControlService.getCabinetLock());
        response.put("distanceThreshold", deviceControlService.getDistanceThreshold());
        return ResponseEntity.ok(response);
    }

    // 8. Set Ultrasonic Distance Threshold (default 10.0 cm)
    @PostMapping("/lock/threshold")
    public ResponseEntity<Map<String, Object>> setDistanceThreshold(
            @RequestParam(required = false) Double threshold) {

        if (threshold != null) {
            deviceControlService.setDistanceThreshold(threshold);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("distanceThreshold", deviceControlService.getDistanceThreshold());
        response.put("cabinetLock", deviceControlService.getCabinetLock());
        return ResponseEntity.ok(response);
    }
}
