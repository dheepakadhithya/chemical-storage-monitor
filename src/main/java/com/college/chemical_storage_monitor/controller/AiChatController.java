package com.college.chemical_storage_monitor.controller;

import com.college.chemical_storage_monitor.service.ai.AiChatService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AiChatController {

    private final AiChatService aiChatService;

    public AiChatController(AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    /**
     * Endpoint for the Voice / Chat AI Assistant
     * Receives spoken or typed messages, triggers hardware tools, and returns AI response.
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody(required = false) Map<String, String> request) {
        String message = (request != null && request.containsKey("message")) ? request.get("message") : "";
        Map<String, Object> response = aiChatService.processMessage(message);
        return ResponseEntity.ok(response);
    }
}
