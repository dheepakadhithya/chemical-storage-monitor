package com.college.chemical_storage_monitor.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DeviceControlService {

    // Devices: "fan", "buzzer", "light"
    // Modes: "NORMAL" (auto based on threshold), "STOPPED" (force off until resume), "ON" (force on)
    private final ConcurrentHashMap<String, String> deviceModes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LocalDateTime> lastUpdated = new ConcurrentHashMap<>();
    private volatile int lightBrightness = 100; // PWM brightness 0-100%
    private volatile int buzzerVolume = 100;    // Volume/duty 0-100%
    private volatile String buzzerPattern = "CONTINUOUS"; // CONTINUOUS, URGENT, PULSE, CHIRP
    private volatile int buzzerTone = 2000;     // Tone frequency 500-3500 Hz
    private volatile String cabinetLock = "LOCKED"; // LOCKED (Armed) vs UNLOCKED (Disarmed)
    private volatile double distanceThreshold = 10.0; // Interruption distance trigger limit in cm

    public DeviceControlService() {
        // Default: All devices in NORMAL mode (standard automatic operation)
        deviceModes.put("fan", "NORMAL");
        deviceModes.put("buzzer", "NORMAL");
        deviceModes.put("light", "NORMAL");

        LocalDateTime now = LocalDateTime.now();
        lastUpdated.put("fan", now);
        lastUpdated.put("buzzer", now);
        lastUpdated.put("light", now);
        this.lightBrightness = 100;
        this.buzzerVolume = 100;
        this.buzzerPattern = "CONTINUOUS";
        this.buzzerTone = 2000;
    }

    public Map<String, String> getAllModes() {
        return new HashMap<>(deviceModes);
    }

    public String getMode(String device) {
        if (device == null) return "NORMAL";
        return deviceModes.getOrDefault(device.toLowerCase().trim(), "NORMAL");
    }

    public boolean setMode(String device, String mode) {
        if (device == null || mode == null) return false;
        String devKey = device.toLowerCase().trim();
        String validMode = mode.toUpperCase().trim();

        if (!validMode.equals("NORMAL") && !validMode.equals("STOPPED") && !validMode.equals("ON")) {
            return false;
        }

        if (devKey.equals("fan") || devKey.equals("buzzer") || devKey.equals("light")) {
            deviceModes.put(devKey, validMode);
            lastUpdated.put(devKey, LocalDateTime.now());
            return true;
        }
        return false;
    }

    public LocalDateTime getLastUpdated(String device) {
        if (device == null) return null;
        return lastUpdated.get(device.toLowerCase().trim());
    }

    public int getLightBrightness() {
        return lightBrightness;
    }

    public void setLightBrightness(int brightness) {
        this.lightBrightness = Math.max(0, Math.min(100, brightness));
        lastUpdated.put("light", LocalDateTime.now());
    }

    public int getBuzzerVolume() {
        return buzzerVolume;
    }

    public void setBuzzerVolume(int volume) {
        this.buzzerVolume = Math.max(0, Math.min(100, volume));
        lastUpdated.put("buzzer", LocalDateTime.now());
    }

    public String getBuzzerPattern() {
        return buzzerPattern;
    }

    public void setBuzzerPattern(String pattern) {
        if (pattern != null) {
            String p = pattern.trim().toUpperCase();
            if (p.equals("CONTINUOUS") || p.equals("URGENT") || p.equals("PULSE") || p.equals("CHIRP")) {
                this.buzzerPattern = p;
                lastUpdated.put("buzzer", LocalDateTime.now());
            }
        }
    }

    public int getBuzzerTone() {
        return buzzerTone;
    }

    public void setBuzzerTone(int tone) {
        this.buzzerTone = Math.max(300, Math.min(5000, tone));
        lastUpdated.put("buzzer", LocalDateTime.now());
    }

    public String getCabinetLock() {
        return cabinetLock;
    }

    public void setCabinetLock(String lock) {
        if (lock != null) {
            String clean = lock.trim().toUpperCase();
            if (clean.equals("LOCKED") || clean.equals("UNLOCKED")) {
                this.cabinetLock = clean;
                lastUpdated.put("cabinetLock", LocalDateTime.now());
            }
        }
    }

    public boolean isCabinetLocked() {
        return "LOCKED".equalsIgnoreCase(cabinetLock);
    }

    public double getDistanceThreshold() {
        return distanceThreshold;
    }

    public void setDistanceThreshold(double threshold) {
        this.distanceThreshold = Math.max(1.0, Math.min(500.0, threshold));
        lastUpdated.put("distanceThreshold", LocalDateTime.now());
    }
}
