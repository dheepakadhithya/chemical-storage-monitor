package com.college.chemical_storage_monitor;

import com.college.chemical_storage_monitor.repository.ChemicalRepository;
import com.college.chemical_storage_monitor.repository.ReadingRepository;
import com.college.chemical_storage_monitor.service.DeviceControlService;
import com.college.chemical_storage_monitor.service.ai.AiChatService;
import com.college.chemical_storage_monitor.service.ai.CabinetControlTools;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class AiChatServiceTest {

    private DeviceControlService deviceControlService;
    private ReadingRepository readingRepository;
    private ChemicalRepository chemicalRepository;
    private CabinetControlTools cabinetControlTools;
    private AiChatService aiChatService;

    @BeforeEach
    void setUp() {
        deviceControlService = new DeviceControlService();
        readingRepository = Mockito.mock(ReadingRepository.class);
        chemicalRepository = Mockito.mock(ChemicalRepository.class);

        when(readingRepository.findTopByOrderByCreatedAtDesc()).thenReturn(null);
        when(chemicalRepository.findAll()).thenReturn(Collections.emptyList());

        cabinetControlTools = new CabinetControlTools(deviceControlService, readingRepository, chemicalRepository);
        aiChatService = new AiChatService(cabinetControlTools, readingRepository, chemicalRepository);
    }

    @Test
    void testFanControlIntent() {
        assertEquals("NORMAL", deviceControlService.getMode("fan"));

        // Test phrase 1: natural request
        Map<String, Object> result1 = aiChatService.processMessage("It feels warm in the lab, please turn on the fan.");
        assertNotNull(result1);
        assertEquals("ON", deviceControlService.getMode("fan"), "Fan should have switched to ON");

        // Test phrase 2: quick chip exact phrase
        Map<String, Object> result2 = aiChatService.processMessage("Please turn on the cooling fan.");
        assertNotNull(result2);
        assertEquals("ON", deviceControlService.getMode("fan"));

        // Test phrase 3: turn off fan
        Map<String, Object> result3 = aiChatService.processMessage("Turn off the fan");
        assertNotNull(result3);
        assertEquals("STOPPED", deviceControlService.getMode("fan"));
    }

    @Test
    void testCabinetLockIntent() {
        deviceControlService.setCabinetLock("LOCKED");

        // Unlock command
        Map<String, Object> result = aiChatService.processMessage("Please unlock cabinet for technician access.");
        assertNotNull(result);
        assertEquals("UNLOCKED", deviceControlService.getCabinetLock(), "Cabinet lock should be UNLOCKED");

        // Lock command
        aiChatService.processMessage("Lock the cabinet.");
        assertEquals("LOCKED", deviceControlService.getCabinetLock(), "Cabinet lock should be LOCKED");
    }

    @Test
    void testBuzzerControlIntent() {
        // Quick chip: Silence the buzzer alarm
        deviceControlService.setMode("buzzer", "ON");
        Map<String, Object> result1 = aiChatService.processMessage("Silence the buzzer alarm.");
        assertNotNull(result1);
        assertEquals("STOPPED", deviceControlService.getMode("buzzer"));

        // Sound / test alarm
        Map<String, Object> result2 = aiChatService.processMessage("Sound the alarm for emergency drill");
        assertNotNull(result2);
        assertEquals("ON", deviceControlService.getMode("buzzer"));
    }

    @Test
    void testLightControlIntent() {
        // Turn on light
        Map<String, Object> result1 = aiChatService.processMessage("Turn on the cabinet lights");
        assertNotNull(result1);
        assertEquals("ON", deviceControlService.getMode("light"));

        // Turn off light
        Map<String, Object> result2 = aiChatService.processMessage("Turn off lights");
        assertNotNull(result2);
        assertEquals("STOPPED", deviceControlService.getMode("light"));
    }
}
