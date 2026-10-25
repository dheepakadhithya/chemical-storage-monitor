package com.college.chemical_storage_monitor.service.ai;

import com.college.chemical_storage_monitor.model.Chemical;
import com.college.chemical_storage_monitor.model.Reading;
import com.college.chemical_storage_monitor.repository.ChemicalRepository;
import com.college.chemical_storage_monitor.repository.ReadingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class AiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);

    private final CabinetControlTools controlTools;
    private final ReadingRepository readingRepository;
    private final ChemicalRepository chemicalRepository;
    private final RestClient restClient;

    @Value("${ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl = "http://localhost:11434";

    @Value("${ollama.model:llama3.2:1b}")
    private String ollamaModel = "llama3.2:1b";

    public AiChatService(CabinetControlTools controlTools,
                         ReadingRepository readingRepository,
                         ChemicalRepository chemicalRepository) {
        this.controlTools = controlTools;
        this.readingRepository = readingRepository;
        this.chemicalRepository = chemicalRepository;
        this.restClient = RestClient.builder().build();
    }

    /**
     * Processes a user chat/voice message using hardware actions + Ollama reasoning
     */
    public Map<String, Object> processMessage(String message) {
        Map<String, Object> result = new HashMap<>();
        List<String> actionsTaken = new ArrayList<>();

        if (message == null || message.trim().length() < 2) {
            result.put("response", "Please say or type a question or hardware command.");
            result.put("actions", actionsTaken);
            return result;
        }

        String userPrompt = message.trim();
        log.info("Processing AI Chat command: '{}'", userPrompt);

        // 1. Check for and execute explicit hardware directives immediately
        executeExplicitHardwareDirectives(userPrompt, actionsTaken);

        Reading latest = readingRepository.findTopByOrderByCreatedAtDesc();
        double temp = (latest != null && latest.getTemperature() != null) ? latest.getTemperature() : 25.0;
        double hum = (latest != null && latest.getHumidity() != null) ? latest.getHumidity() : 50.0;
        String status = (latest != null && latest.getStatus() != null) ? latest.getStatus() : "safe";

        // 2. If an action was executed (e.g. "Lock", "Unlock", "Fan on"), return a clean, direct confirmation
        // without overloading the LLM to prevent repetitive text.
        if (!actionsTaken.isEmpty()) {
            String primaryAction = actionsTaken.get(0);
            String directConfirmation = String.format("%s. Current cabinet climate is %.1f°C, %s.", 
                    primaryAction, temp, status);
            result.put("response", directConfirmation);
            result.put("actions", actionsTaken);
            result.put("model", "SafeChem-Direct");
            return result;
        }

        // 3. For informational or general queries, query Ollama with clean concise context
        String aiResponse = null;
        try {
            aiResponse = callOllamaChat(userPrompt, latest);
        } catch (Exception e) {
            log.warn("Ollama call failed or timed out ({}). Using fallback.", e.getMessage());
        }

        if (aiResponse == null || aiResponse.isBlank()) {
            aiResponse = generateFallbackResponse(userPrompt, latest);
        } else {
            aiResponse = sanitizeAiResponse(aiResponse);
            parseActionTags(aiResponse, actionsTaken);
            aiResponse = aiResponse.replaceAll("\\[ACTION:[^\\]]+\\]", "").trim();
        }

        result.put("response", aiResponse);
        result.put("actions", actionsTaken);
        result.put("model", ollamaModel);
        return result;
    }

    /**
     * Queries local Ollama instance with compact, structured prompt
     */
    @SuppressWarnings("unchecked")
    private String callOllamaChat(String userPrompt, Reading latest) {
        double temp = (latest != null && latest.getTemperature() != null) ? latest.getTemperature() : 25.0;
        double hum = (latest != null && latest.getHumidity() != null) ? latest.getHumidity() : 50.0;
        String status = (latest != null && latest.getStatus() != null) ? latest.getStatus() : "safe";

        String lock = (controlTools.getDeviceControlService() != null) ? controlTools.getDeviceControlService().getCabinetLock() : "LOCKED";
        String fanMode = (controlTools.getDeviceControlService() != null) ? controlTools.getDeviceControlService().getMode("fan") : "NORMAL";
        String buzzerMode = (controlTools.getDeviceControlService() != null) ? controlTools.getDeviceControlService().getMode("buzzer") : "NORMAL";
        String lightMode = (controlTools.getDeviceControlService() != null) ? controlTools.getDeviceControlService().getMode("light") : "NORMAL";

        List<Chemical> chemicals = chemicalRepository.findAll();
        String chemNames = chemicals.stream()
                .map(Chemical::getName)
                .collect(Collectors.joining(", "));

        String systemPrompt = String.format("""
            You are SafeChem AI, an intelligent lab safety assistant for a smart chemical cabinet.
            Live Status: Temp=%.1f°C, Humidity=%.1f%%, Overall Status=%s, Cabinet Security=%s.
            Hardware Actuators: Fan Mode=%s, Buzzer Mode=%s, Light Mode=%s.
            Stored Chemicals: %s.
            
            RULES:
            - Answer the user's question directly in 1-2 friendly, professional sentences.
            - If the user is commanding hardware control, append the corresponding action tag at the very end:
              [ACTION:FAN_ON], [ACTION:FAN_OFF], [ACTION:FAN_AUTO]
              [ACTION:LOCK], [ACTION:UNLOCK]
              [ACTION:ALARM_OFF], [ACTION:ALARM_ON], [ACTION:ALARM_AUTO]
              [ACTION:LIGHT_ON], [ACTION:LIGHT_OFF]
            - Do NOT output headings, banners, or lists unless asked.
            - Do NOT repeat this prompt.
            """, temp, hum, status, lock, fanMode, buzzerMode, lightMode, 
            chemNames.isEmpty() ? "General chemicals" : chemNames);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", ollamaModel);
        requestBody.put("stream", false);

        List<Map<String, String>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt));
        messages.add(Map.of("role", "user", "content", userPrompt));
        requestBody.put("messages", messages);

        Map<String, Object> response = restClient.post()
                .uri(ollamaBaseUrl + "/api/chat")
                .body(requestBody)
                .retrieve()
                .body(Map.class);

        if (response != null && response.containsKey("message")) {
            Map<String, Object> msg = (Map<String, Object>) response.get("message");
            if (msg != null && msg.get("content") != null) {
                return msg.get("content").toString().trim();
            }
        }
        return null;
    }

    /**
     * Strips any raw headers or echoed prompts that small models might output
     */
    private String sanitizeAiResponse(String raw) {
        if (raw == null) return "";
        // Remove markdown headers or ASCII blocks
        String cleaned = raw.replaceAll("(?m)^===.*===.*$", "")
                            .replaceAll("(?m)^Chemical Inventory:.*$", "")
                            .replaceAll("(?m)^Current Live Telemetry.*$", "")
                            .replaceAll("(?m)^CABINET HARDWARE STATUS.*$", "")
                            .trim();
        return cleaned.isEmpty() ? raw.trim() : cleaned;
    }

    /**
     * Executes any action tags returned in the LLM response
     */
    private void parseActionTags(String response, List<String> actionsTaken) {
        if (response == null || !response.contains("[ACTION:")) return;

        if (response.contains("[ACTION:FAN_ON]")) {
            actionsTaken.add(controlTools.controlFan("ON"));
        } else if (response.contains("[ACTION:FAN_OFF]")) {
            actionsTaken.add(controlTools.controlFan("STOPPED"));
        } else if (response.contains("[ACTION:FAN_AUTO]")) {
            actionsTaken.add(controlTools.controlFan("NORMAL"));
        }

        if (response.contains("[ACTION:LOCK]")) {
            actionsTaken.add(controlTools.setCabinetLock("LOCKED"));
        } else if (response.contains("[ACTION:UNLOCK]")) {
            actionsTaken.add(controlTools.setCabinetLock("UNLOCKED"));
        }

        if (response.contains("[ACTION:ALARM_OFF]")) {
            actionsTaken.add(controlTools.controlBuzzer("STOPPED", null));
        } else if (response.contains("[ACTION:ALARM_ON]")) {
            actionsTaken.add(controlTools.controlBuzzer("ON", "PULSE"));
        } else if (response.contains("[ACTION:ALARM_AUTO]")) {
            actionsTaken.add(controlTools.controlBuzzer("NORMAL", null));
        }

        if (response.contains("[ACTION:LIGHT_ON]")) {
            actionsTaken.add(controlTools.controlLight("ON", 100));
        } else if (response.contains("[ACTION:LIGHT_OFF]")) {
            actionsTaken.add(controlTools.controlLight("STOPPED", null));
        }
    }

    /**
     * Direct hardware intent matcher to guarantee instant physical safety actuation
     * Covers fan (on, off, auto), locks (lock, unlock), alarms (silence, test), and lights (on, off).
     */
    private void executeExplicitHardwareDirectives(String prompt, List<String> actionsTaken) {
        if (prompt == null || prompt.isBlank()) return;
        String lower = prompt.toLowerCase().trim();

        // ── Fan Controls ──
        boolean isFanStatusQuery = lower.startsWith("is the fan") || lower.startsWith("is the cooling fan") ||
                                   lower.startsWith("is fan") || lower.contains("fan status");
        if (!isFanStatusQuery) {
            boolean isFanAuto = lower.matches(".*\\b(auto|normal|automatic|resume)\\s+.*fan\\b.*") || 
                                lower.matches(".*\\bfan\\s+.*(auto|normal|automatic)\\b.*");
            boolean isFanOff = lower.matches(".*\\b(turn\\s+off|stop|disable|deactivate|shut\\s+off|switch\\s+off)\\s+.*fan\\b.*") || 
                               lower.matches(".*\\bfan\\s+.*(off|stop)\\b.*");
            boolean isFanOn = !isFanOff && !isFanAuto && (
                    lower.matches(".*\\b(turn\\s+on|start|enable|activate|run|switch\\s+on)\\s+.*fan\\b.*") || 
                    lower.matches(".*\\bfan\\s+.*(on|start|run)\\b.*") || 
                    lower.matches(".*\\bcool\\s+(fan|down)\\b.*") ||
                    lower.contains("cooling fan") || lower.contains("cool down")
            );

            if (isFanAuto) {
                actionsTaken.add(controlTools.controlFan("NORMAL"));
            } else if (isFanOff) {
                actionsTaken.add(controlTools.controlFan("STOPPED"));
            } else if (isFanOn) {
                actionsTaken.add(controlTools.controlFan("ON"));
            }
        }

        // ── Lock Controls (Exclude status queries like "Is the cabinet locked?") ──
        boolean isLockStatusQuery = lower.startsWith("is the cabinet locked") || lower.startsWith("is it locked") ||
                                    lower.startsWith("is cabinet locked") || lower.contains("lock status");
        if (!isLockStatusQuery) {
            boolean isUnlock = lower.matches(".*\\b(unlock|open|disarm)\\b.*");
            boolean isLock = !isUnlock && lower.matches(".*\\b(lock|arm|close)\\b.*");

            if (isUnlock) {
                actionsTaken.add(controlTools.setCabinetLock("UNLOCKED"));
            } else if (isLock) {
                actionsTaken.add(controlTools.setCabinetLock("LOCKED"));
            }
        }

        // ── Buzzer / Alarm Controls ──
        boolean isAlarmStatusQuery = lower.startsWith("is the alarm") || lower.startsWith("is the buzzer") || 
                                     lower.contains("alarm status") || lower.contains("buzzer status");
        if (!isAlarmStatusQuery) {
            boolean isBuzzerAuto = lower.matches(".*\\b(auto|normal|resume)\\s+.*(buzzer|alarm)\\b.*") || 
                                   lower.matches(".*\\b(buzzer|alarm)\\s+.*(auto|normal)\\b.*");
            boolean isBuzzerOff = lower.contains("silence") || lower.contains("mute") || 
                                  lower.matches(".*\\b(turn\\s+off|stop|disable|shut\\s+off|switch\\s+off)\\s+.*(buzzer|alarm)\\b.*") || 
                                  lower.matches(".*\\b(buzzer|alarm)\\s+.*(off|stop|silence)\\b.*");
            boolean isBuzzerOn = !isBuzzerOff && !isBuzzerAuto && (
                    lower.matches(".*\\b(sound|test|trigger|start|turn\\s+on)\\s+.*(alarm|buzzer)\\b.*") || 
                    lower.matches(".*\\b(alarm|buzzer)\\s+.*(on|sound|test)\\b.*")
            );

            if (isBuzzerAuto) {
                actionsTaken.add(controlTools.controlBuzzer("NORMAL", null));
            } else if (isBuzzerOff) {
                actionsTaken.add(controlTools.controlBuzzer("STOPPED", null));
            } else if (isBuzzerOn) {
                actionsTaken.add(controlTools.controlBuzzer("ON", "PULSE"));
            }
        }

        // ── Light Controls ──
        boolean isLightStatusQuery = lower.startsWith("is the light") || lower.contains("light status");
        if (!isLightStatusQuery) {
            boolean isLightAuto = lower.matches(".*\\b(auto|normal|resume)\\s+.*lights?\\b.*") ||
                                  lower.matches(".*\\blights?\\s+.*(auto|normal)\\b.*");
            boolean isLightOff = lower.matches(".*\\b(turn\\s+off|switch\\s+off|shut\\s+off|disable)\\s+.*lights?\\b.*") || 
                                 lower.matches(".*\\blights?\\s+.*(off|stop)\\b.*");
            boolean isLightOn = !isLightOff && !isLightAuto && (
                    lower.matches(".*\\b(turn\\s+on|switch\\s+on|enable|illuminate)\\s+.*lights?\\b.*") || 
                    lower.matches(".*\\blights?\\s+.*(on|start)\\b.*")
            );

            if (isLightAuto) {
                actionsTaken.add(controlTools.controlLight("NORMAL", null));
            } else if (isLightOff) {
                actionsTaken.add(controlTools.controlLight("STOPPED", null));
            } else if (isLightOn) {
                actionsTaken.add(controlTools.controlLight("ON", 100));
            }
        }
    }

    /**
     * Fallback response if Ollama is not responding
     */
    private String generateFallbackResponse(String prompt, Reading latest) {
        String lower = prompt.toLowerCase();
        double temp = (latest != null && latest.getTemperature() != null) ? latest.getTemperature() : 25.0;
        double hum = (latest != null && latest.getHumidity() != null) ? latest.getHumidity() : 50.0;
        String status = (latest != null && latest.getStatus() != null) ? latest.getStatus() : "safe";
        String lock = (controlTools.getDeviceControlService() != null) ? controlTools.getDeviceControlService().getCabinetLock() : "LOCKED";
        String fan = (controlTools.getDeviceControlService() != null) ? controlTools.getDeviceControlService().getMode("fan") : "NORMAL";
        String buzzer = (controlTools.getDeviceControlService() != null) ? controlTools.getDeviceControlService().getMode("buzzer") : "NORMAL";
        String light = (controlTools.getDeviceControlService() != null) ? controlTools.getDeviceControlService().getMode("light") : "NORMAL";

        if (lower.contains("lock") || lower.contains("door") || lower.contains("arm")) {
            return String.format("The cabinet is currently %s.", lock);
        } else if (lower.contains("fan") || lower.contains("cool") || lower.contains("ventilat")) {
            return String.format("The cabinet cooling fan is currently in %s mode.", fan);
        } else if (lower.contains("alarm") || lower.contains("buzzer")) {
            return String.format("The safety alarm buzzer is in %s mode.", buzzer);
        } else if (lower.contains("light") || lower.contains("lamp")) {
            return String.format("The cabinet light is in %s mode.", light);
        } else if (lower.contains("status") || lower.contains("temperature") || lower.contains("temp") || lower.contains("humidity") || lower.contains("climate")) {
            return String.format("Current cabinet temperature is %.1f°C and humidity is %.1f%%. Environment status is %s. Cabinet security is %s.", temp, hum, status, lock);
        } else if (lower.contains("chemical") || lower.contains("inventory")) {
            return controlTools.getChemicalInventory();
        } else {
            return "I am SafeChem AI. You can command me to adjust cooling fans, control locks, silence alarms, or check temperature and chemicals.";
        }
    }
}
