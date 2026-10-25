package com.college.chemical_storage_monitor.service.ai;

import com.college.chemical_storage_monitor.model.Chemical;
import com.college.chemical_storage_monitor.model.Reading;
import com.college.chemical_storage_monitor.repository.ChemicalRepository;
import com.college.chemical_storage_monitor.repository.ReadingRepository;
import com.college.chemical_storage_monitor.service.DeviceControlService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Hardware and Data Tools that the AI Agent can execute.
 * Enables the LLM to inspect live sensor telemetry and actuate physical cabinet devices.
 */
@Service
public class CabinetControlTools {

    private final DeviceControlService deviceControlService;
    private final ReadingRepository readingRepository;
    private final ChemicalRepository chemicalRepository;

    public CabinetControlTools(DeviceControlService deviceControlService,
                               ReadingRepository readingRepository,
                               ChemicalRepository chemicalRepository) {
        this.deviceControlService = deviceControlService;
        this.readingRepository = readingRepository;
        this.chemicalRepository = chemicalRepository;
    }

    public DeviceControlService getDeviceControlService() {
        return deviceControlService;
    }

    /**
     * Controls the cooling fan inside the chemical cabinet.
     * @param mode "ON" (force on), "STOPPED" (force off), or "NORMAL" (automatic based on temperature)
     */
    public String controlFan(String mode) {
        String cleanMode = (mode == null ? "NORMAL" : mode.trim().toUpperCase());
        if (!cleanMode.equals("ON") && !cleanMode.equals("STOPPED") && !cleanMode.equals("NORMAL")) {
            cleanMode = "NORMAL";
        }
        boolean success = deviceControlService.setMode("fan", cleanMode);
        if (success) {
            return "The cabinet cooling fan has been switched to mode: " + cleanMode;
        } else {
            return "Failed to set fan mode to " + cleanMode;
        }
    }

    /**
     * Controls the security buzzer.
     * @param mode "ON", "STOPPED", or "NORMAL"
     * @param pattern Optional: "CONTINUOUS", "URGENT", "PULSE", "CHIRP"
     */
    public String controlBuzzer(String mode, String pattern) {
        String cleanMode = (mode == null ? "NORMAL" : mode.trim().toUpperCase());
        if (pattern != null && !pattern.isBlank()) {
            deviceControlService.setBuzzerPattern(pattern.toUpperCase());
        }
        boolean success = deviceControlService.setMode("buzzer", cleanMode);
        return success ? "Cabinet buzzer switched to " + cleanMode + (pattern != null ? " with pattern " + pattern : "")
                       : "Failed to set buzzer mode.";
    }

    /**
     * Controls the cabinet internal illumination.
     * @param mode "ON", "STOPPED", or "NORMAL"
     * @param brightness Brightness percentage (0 to 100)
     */
    public String controlLight(String mode, Integer brightness) {
        String cleanMode = (mode == null ? "NORMAL" : mode.trim().toUpperCase());
        if (brightness != null) {
            deviceControlService.setLightBrightness(brightness);
        }
        boolean success = deviceControlService.setMode("light", cleanMode);
        return success ? "Cabinet light switched to " + cleanMode + (brightness != null ? " at " + brightness + "% brightness" : "")
                       : "Failed to update light.";
    }

    /**
     * Arms (LOCKED) or Disarms (UNLOCKED) the cabinet security lock.
     * @param state "LOCKED" or "UNLOCKED"
     */
    public String setCabinetLock(String state) {
        String cleanState = (state == null ? "LOCKED" : state.trim().toUpperCase());
        if (!cleanState.equals("LOCKED") && !cleanState.equals("UNLOCKED")) {
            cleanState = "LOCKED";
        }
        deviceControlService.setCabinetLock(cleanState);
        return "Cabinet security lock state updated to: " + cleanState;
    }

    /**
     * Retrieves the latest live DHT11 temperature, humidity, ultrasonic distance, and actuator modes.
     */
    public String getCabinetStatus() {
        Reading latest = readingRepository.findTopByOrderByCreatedAtDesc();
        StringBuilder sb = new StringBuilder();
        sb.append("=== CABINET LIVE TELEMETRY & HARDWARE STATUS ===\n");
        if (latest != null) {
            sb.append(String.format("Temperature: %.1f°C\n", latest.getTemperature()));
            sb.append(String.format("Humidity: %.1f%%\n", latest.getHumidity()));
            sb.append(String.format("Distance: %.1f cm (Threshold: %.1f cm)\n", 
                    latest.getDistance() != null ? latest.getDistance() : 0.0,
                    latest.getDistanceThreshold() != null ? latest.getDistanceThreshold() : 10.0));
            sb.append(String.format("Overall Status: %s\n", latest.getStatus()));
            if (latest.getChemicalName() != null) {
                sb.append(String.format("Monitored Chemical: %s\n", latest.getChemicalName()));
            }
            if (latest.getAlertReason() != null && !latest.getAlertReason().isBlank()) {
                sb.append(String.format("Active Alert: %s\n", latest.getAlertReason()));
            }
        } else {
            sb.append("No telemetry readings recorded yet in database.\n");
        }

        sb.append(String.format("Fan Mode: %s\n", deviceControlService.getMode("fan")));
        sb.append(String.format("Buzzer Mode: %s (Pattern: %s)\n", 
                deviceControlService.getMode("buzzer"), deviceControlService.getBuzzerPattern()));
        sb.append(String.format("Cabinet Lock: %s\n", deviceControlService.getCabinetLock()));
        sb.append(String.format("Light Mode: %s (Brightness: %d%%)\n", 
                deviceControlService.getMode("light"), deviceControlService.getLightBrightness()));

        return sb.toString();
    }

    /**
     * Retrieves the inventory of stored chemicals and their safety limits.
     */
    public String getChemicalInventory() {
        List<Chemical> chemicals = chemicalRepository.findAll();
        if (chemicals.isEmpty()) {
            return "No chemicals are currently registered in the cabinet inventory.";
        }

        StringBuilder sb = new StringBuilder("=== STORED CHEMICAL INVENTORY ===\n");
        for (Chemical c : chemicals) {
            sb.append(String.format("- %s (%s) | Hazard: %s | Max Safe Temp: %.1f°C | Max Humidity: %.1f%% | Active: %s\n",
                    c.getName(),
                    c.getFormula() != null ? c.getFormula() : "N/A",
                    c.getHazardCategory() != null ? c.getHazardCategory() : "General",
                    c.getMaxTemperature() != null ? c.getMaxTemperature() : 30.0,
                    c.getMaxHumidity() != null ? c.getMaxHumidity() : 70.0,
                    c.isActive() ? "YES" : "NO"));
        }
        return sb.toString();
    }
}
