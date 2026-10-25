package com.college.chemical_storage_monitor.service;

import com.college.chemical_storage_monitor.model.Chemical;
import com.college.chemical_storage_monitor.model.Reading;
import com.college.chemical_storage_monitor.repository.ReadingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class ReadingService {

    private final ReadingRepository readingRepository;
    private final ChemicalService chemicalService;
    private final DeviceControlService deviceControlService;

    // Buffer for the last 5 normal readings
    private final ConcurrentLinkedDeque<Reading> normalBuffer = new ConcurrentLinkedDeque<>();
    private static final int BUFFER_SIZE = 5;

    // Keep track of the latest reading overall
    private final AtomicReference<Reading> latestReading = new AtomicReference<>();

    // Keep track of the latest reading per chemical ID
    private final ConcurrentHashMap<Long, Reading> latestReadingsByChemical = new ConcurrentHashMap<>();

    // State tracker for danger events
    private boolean isDangerState = false;

    private static final List<String> DANGER_STATUSES = java.util.Arrays.asList(
            "danger", "interruption_danger", "both_danger", "environmental_danger"
    );

    @Autowired
    public ReadingService(ReadingRepository readingRepository,
                          ChemicalService chemicalService,
                          DeviceControlService deviceControlService) {
        this.readingRepository = readingRepository;
        this.chemicalService = chemicalService;
        this.deviceControlService = deviceControlService;
    }

    public synchronized Reading processNewReading(Reading reading) {
        // Attach current actuator modes and lock states to the reading response for the microcontroller
        double distLimit = 10.0;
        boolean cabinetLocked = true;
        if (deviceControlService != null) {
            reading.setFanControl(deviceControlService.getMode("fan"));
            reading.setBuzzerControl(deviceControlService.getMode("buzzer"));
            reading.setLightControl(deviceControlService.getMode("light"));
            reading.setLightBrightness(deviceControlService.getLightBrightness());
            reading.setBuzzerVolume(deviceControlService.getBuzzerVolume());
            reading.setBuzzerPattern(deviceControlService.getBuzzerPattern());
            reading.setBuzzerTone(deviceControlService.getBuzzerTone());
            reading.setCabinetLock(deviceControlService.getCabinetLock());
            distLimit = deviceControlService.getDistanceThreshold();
            cabinetLocked = deviceControlService.isCabinetLocked();
        }
        reading.setDistanceThreshold(distLimit);

        // Resolve chemical: use reading's chemicalId if provided, else use active chemical
        Chemical targetChem = null;
        if (reading.getChemicalId() != null) {
            targetChem = chemicalService.getChemicalById(reading.getChemicalId()).orElse(null);
        }
        if (targetChem == null) {
            targetChem = chemicalService.getActiveChemical();
        }

        double tempLimit = (targetChem != null && targetChem.getMaxTemperature() != null)
                ? targetChem.getMaxTemperature() : 50.00;
        double humLimit = (targetChem != null && targetChem.getMaxHumidity() != null)
                ? targetChem.getMaxHumidity() : 85.00;

        reading.setChemicalId(targetChem != null ? targetChem.getId() : null);
        reading.setChemicalName(targetChem != null ? targetChem.getName() : "General Chemical");
        reading.setTempThreshold(tempLimit);
        reading.setHumidityThreshold(humLimit);

        // 1. Environmental Danger Check (Temperature & Humidity)
        boolean tempDanger = reading.getTemperature() != null && reading.getTemperature() >= tempLimit;
        boolean humDanger = reading.getHumidity() != null && reading.getHumidity() > humLimit;
        boolean envDanger = tempDanger || humDanger;

        // 2. Ultrasonic Interruption Danger Check (Only active when Cabinet is Locked)
        boolean interruptionDanger = false;
        if (cabinetLocked) {
            Double curDist = reading.getDistance();
            boolean distanceBreach = curDist != null && curDist > 0.0 && curDist <= distLimit;
            boolean flagBreach = Boolean.TRUE.equals(reading.getInterruption());
            interruptionDanger = distanceBreach || flagBreach;
        }
        reading.setInterruption(interruptionDanger);

        // 3. Determine Dual Danger Status and Reason
        StringBuilder reason = new StringBuilder();
        if (envDanger && interruptionDanger) {
            reading.setStatus("both_danger");
            reading.setDangerType("BOTH");
            if (tempDanger) {
                reason.append(String.format("Temp (%.1f°C >= limit %.1f°C). ", reading.getTemperature(), tempLimit));
            }
            if (humDanger) {
                reason.append(String.format("Humidity (%.1f%% > limit %.1f%%). ", reading.getHumidity(), humLimit));
            }
            reason.append(String.format("Cabinet Interruption! Obstacle at %.1f cm (<= %.1f cm limit). ", 
                    reading.getDistance() != null ? reading.getDistance() : 0.0, distLimit));
        } else if (interruptionDanger) {
            reading.setStatus("interruption_danger");
            reading.setDangerType("INTERRUPTION");
            reason.append(String.format("Cabinet Interruption Detected! Ultrasonic distance: %.1f cm (threshold: %.1f cm, Cabinet Locked). ",
                    reading.getDistance() != null ? reading.getDistance() : 0.0, distLimit));
        } else if (envDanger) {
            reading.setStatus("danger");
            reading.setDangerType("ENVIRONMENTAL");
            if (tempDanger) {
                reason.append(String.format("Temp (%.1f°C >= limit %.1f°C). ", reading.getTemperature(), tempLimit));
            }
            if (humDanger) {
                reason.append(String.format("Humidity (%.1f%% > limit %.1f%%). ", reading.getHumidity(), humLimit));
            }
        } else {
            reading.setStatus("safe");
            reading.setDangerType("NONE");
        }


        if (reason.length() > 0) {
            reading.setAlertReason(reason.toString().trim());
        } else {
            reading.setAlertReason(null);
        }

        // Update the in-memory latest readings
        latestReading.set(reading);
        if (reading.getChemicalId() != null) {
            latestReadingsByChemical.put(reading.getChemicalId(), reading);
        }

        // Propagate current sensor reading to all registered chemicals so non-active dashboards also render live values
        try {
            List<Chemical> allChems = chemicalService.getAllChemicals();
            for (Chemical c : allChems) {
                if (c.getId() == null) continue;
                if (targetChem == null || !c.getId().equals(targetChem.getId())) {
                    latestReadingsByChemical.put(c.getId(), createChemicalReadingSnapshot(reading, c));
                }
            }
        } catch (Exception e) {
            // Keep going if chemicalService encounters error
        }

        boolean isDanger = envDanger || interruptionDanger;

        if (isDanger) {
            if (!isDangerState) {
                // Transitioning from normal to danger:
                // Save the 5 data points before the danger limit
                if (!normalBuffer.isEmpty()) {
                    readingRepository.saveAll(normalBuffer);
                    normalBuffer.clear();
                }
                isDangerState = true;
            }
            // Save the current danger data (and all consecutive danger data)
            return readingRepository.save(reading);

        } else { // Normal/Safe status
            if (isDangerState) {
                // Transitioning back to normal from danger state:
                isDangerState = false;

                // Save this first normal reading to mark the end of the danger event in the DB
                return readingRepository.save(reading);
            } else {
                // Completely normal operation, keep reading transient in buffer (max 5)
                normalBuffer.addLast(reading);
                if (normalBuffer.size() > BUFFER_SIZE) {
                    normalBuffer.removeFirst();
                }

                // Return without saving to DB
                return reading;
            }
        }
    }

    private Reading createChemicalReadingSnapshot(Reading base, Chemical c) {
        Reading r = new Reading();
        r.setTemperature(base.getTemperature());
        r.setHumidity(base.getHumidity());
        r.setDistance(base.getDistance());
        r.setDistanceThreshold(base.getDistanceThreshold());
        r.setInterruption(base.getInterruption());
        r.setCabinetLock(base.getCabinetLock());
        r.setCreatedAt(base.getCreatedAt());
        r.setChemicalId(c.getId());
        r.setChemicalName(c.getName());
        double cTempLimit = c.getMaxTemperature() != null ? c.getMaxTemperature() : 50.0;
        double cHumLimit = c.getMaxHumidity() != null ? c.getMaxHumidity() : 85.0;
        r.setTempThreshold(cTempLimit);
        r.setHumidityThreshold(cHumLimit);

        boolean tDanger = r.getTemperature() != null && r.getTemperature() >= cTempLimit;
        boolean hDanger = r.getHumidity() != null && r.getHumidity() > cHumLimit;
        boolean envDanger = tDanger || hDanger;
        boolean interruptionDanger = Boolean.TRUE.equals(base.getInterruption());

        StringBuilder sb = new StringBuilder();
        if (envDanger && interruptionDanger) {
            r.setStatus("both_danger");
            r.setDangerType("BOTH");
            if (tDanger) sb.append(String.format("Temp (%.1f°C >= limit %.1f°C). ", r.getTemperature(), cTempLimit));
            if (hDanger) sb.append(String.format("Humidity (%.1f%% > limit %.1f%%). ", r.getHumidity(), cHumLimit));
            sb.append("Cabinet Interruption Detected. ");
        } else if (interruptionDanger) {
            r.setStatus("interruption_danger");
            r.setDangerType("INTERRUPTION");
            sb.append("Cabinet Interruption Detected. ");
        } else if (envDanger) {
            r.setStatus("danger");
            r.setDangerType("ENVIRONMENTAL");
            if (tDanger) sb.append(String.format("Temp (%.1f°C >= limit %.1f°C). ", r.getTemperature(), cTempLimit));
            if (hDanger) sb.append(String.format("Humidity (%.1f%% > limit %.1f%%). ", r.getHumidity(), cHumLimit));
        } else {
            r.setStatus("safe");
            r.setDangerType("NONE");
        }

        if (sb.length() > 0) {
            r.setAlertReason(sb.toString().trim());
        } else {
            r.setAlertReason(null);
        }
        return r;
    }

    public Reading getLatestReading() {
        return latestReading.get();
    }

    public Reading getLatestReading(Long chemicalId) {
        if (chemicalId == null) {
            return latestReading.get();
        }
        Reading memReading = latestReadingsByChemical.get(chemicalId);
        if (memReading != null) {
            return memReading;
        }
        Reading live = latestReading.get();
        if (live != null) {
            Chemical chem = chemicalService.getChemicalById(chemicalId).orElse(null);
            if (chem != null) {
                Reading snapshot = createChemicalReadingSnapshot(live, chem);
                latestReadingsByChemical.put(chemicalId, snapshot);
                return snapshot;
            }
            return live;
        }
        // Fallback to database
        return readingRepository.findTopByChemicalIdOrderByCreatedAtDesc(chemicalId);
    }

    public Map<Long, Reading> getAllLatestReadings() {
        Reading live = latestReading.get();
        if (live != null) {
            try {
                List<Chemical> allChems = chemicalService.getAllChemicals();
                for (Chemical c : allChems) {
                    if (c.getId() != null && !latestReadingsByChemical.containsKey(c.getId())) {
                        latestReadingsByChemical.put(c.getId(), createChemicalReadingSnapshot(live, c));
                    }
                }
            } catch (Exception ignored) {}
        }
        return latestReadingsByChemical;
    }

    public List<Reading> getAlerts() {
        return readingRepository.findByStatusInOrderByCreatedAtDesc(DANGER_STATUSES);
    }

    public List<Reading> getAlerts(Long chemicalId) {
        if (chemicalId == null) {
            return getAlerts();
        }
        return readingRepository.findByChemicalIdAndStatusInOrderByCreatedAtDesc(chemicalId, DANGER_STATUSES);
    }

    public List<Reading> getFullHistory() {
        return readingRepository.findAllByOrderByCreatedAtDesc();
    }

    public List<Reading> getFullHistory(Long chemicalId) {
        if (chemicalId == null) {
            return getFullHistory();
        }
        return readingRepository.findByChemicalIdOrderByCreatedAtDesc(chemicalId);
    }
}
