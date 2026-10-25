package com.college.chemical_storage_monitor.controller;

import com.college.chemical_storage_monitor.model.Chemical;
import com.college.chemical_storage_monitor.service.ChemicalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chemicals")
@CrossOrigin(origins = "*")
public class ChemicalController {

    private final ChemicalService chemicalService;

    @Autowired
    public ChemicalController(ChemicalService chemicalService) {
        this.chemicalService = chemicalService;
    }

    // 1. Get all chemicals (Viewer, Operator, Admin)
    @GetMapping
    public ResponseEntity<List<Chemical>> getAllChemicals() {
        return ResponseEntity.ok(chemicalService.getAllChemicals());
    }

    // 2. Get active monitored chemical (Permit all / IoT)
    @GetMapping("/active")
    public ResponseEntity<Chemical> getActiveChemical() {
        return ResponseEntity.ok(chemicalService.getActiveChemical());
    }

    // 3. Set active chemical being monitored (Admin & Operator only)
    @PostMapping("/active/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Chemical> setActiveChemical(@PathVariable Long id) {
        Chemical updated = chemicalService.setActiveChemical(id);
        return ResponseEntity.ok(updated);
    }

    // 4. Add a new chemical profile (Admin & Operator only)
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Chemical> createChemical(@RequestBody Chemical chemical) {
        Chemical saved = chemicalService.saveChemical(chemical);
        return ResponseEntity.ok(saved);
    }

    // 5. Update thresholds for a specific chemical (Admin & Operator only)
    @PutMapping("/{id}/thresholds")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Chemical> updateThresholds(
            @PathVariable Long id,
            @RequestBody Map<String, Double> payload) {
        Double maxTemp = payload.get("maxTemperature");
        Double maxHum = payload.get("maxHumidity");
        Chemical updated = chemicalService.updateThresholds(id, maxTemp, maxHum);
        return ResponseEntity.ok(updated);
    }

    // 6. Update complete chemical details and thresholds (Admin & Operator only)
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERATOR')")
    public ResponseEntity<Chemical> updateChemical(
            @PathVariable Long id,
            @RequestBody Chemical chemicalDetails) {
        Chemical updated = chemicalService.updateChemical(id, chemicalDetails);
        return ResponseEntity.ok(updated);
    }

    // 7. Delete a chemical (Admin only)
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteChemical(@PathVariable Long id) {
        chemicalService.deleteChemical(id);
        return ResponseEntity.ok(Map.of("message", "Chemical deleted successfully."));
    }
}
