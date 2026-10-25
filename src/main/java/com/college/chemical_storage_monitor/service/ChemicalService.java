package com.college.chemical_storage_monitor.service;

import com.college.chemical_storage_monitor.model.Chemical;
import com.college.chemical_storage_monitor.repository.ChemicalRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class ChemicalService {

    private final ChemicalRepository chemicalRepository;

    @Autowired
    public ChemicalService(ChemicalRepository chemicalRepository) {
        this.chemicalRepository = chemicalRepository;
    }

    public List<Chemical> getAllChemicals() {
        return chemicalRepository.findAllByOrderByActiveDescNameAsc();
    }

    public Optional<Chemical> getChemicalById(Long id) {
        return chemicalRepository.findById(id);
    }

    public Chemical getActiveChemical() {
        return chemicalRepository.findByActiveTrue()
                .orElseGet(() -> {
                    // Fallback to the first chemical or create default if table empty
                    List<Chemical> all = chemicalRepository.findAll();
                    if (!all.isEmpty()) {
                        Chemical first = all.get(0);
                        first.setActive(true);
                        return chemicalRepository.save(first);
                    }
                    Chemical defaultChem = new Chemical(
                            "General Laboratory Chemical",
                            "N/A",
                            "Standard Storage",
                            35.0,
                            75.0,
                            "Standard climate-controlled laboratory cabinet.",
                            true
                    );
                    return chemicalRepository.save(defaultChem);
                });
    }

    @Transactional
    public Chemical setActiveChemical(Long id) {
        Chemical target = chemicalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Chemical with ID " + id + " not found."));

        // Deactivate all
        List<Chemical> all = chemicalRepository.findAll();
        for (Chemical c : all) {
            c.setActive(c.getId().equals(id));
        }
        chemicalRepository.saveAll(all);

        target.setActive(true);
        return target;
    }

    @Transactional
    public Chemical saveChemical(Chemical chemical) {
        if (chemical.isActive()) {
            // Deactivate others
            List<Chemical> all = chemicalRepository.findAll();
            for (Chemical c : all) {
                c.setActive(false);
            }
            chemicalRepository.saveAll(all);
        }
        return chemicalRepository.save(chemical);
    }

    @Transactional
    public Chemical updateThresholds(Long id, Double maxTemp, Double maxHum) {
        Chemical target = chemicalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Chemical not found with ID: " + id));
        if (maxTemp != null) target.setMaxTemperature(maxTemp);
        if (maxHum != null) target.setMaxHumidity(maxHum);
        return chemicalRepository.save(target);
    }

    @Transactional
    public Chemical updateChemical(Long id, Chemical details) {
        Chemical target = chemicalRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Chemical not found with ID: " + id));
        if (details.getName() != null) target.setName(details.getName());
        if (details.getFormula() != null) target.setFormula(details.getFormula());
        if (details.getHazardCategory() != null) target.setHazardCategory(details.getHazardCategory());
        if (details.getMaxTemperature() != null) target.setMaxTemperature(details.getMaxTemperature());
        if (details.getMaxHumidity() != null) target.setMaxHumidity(details.getMaxHumidity());
        if (details.getStorageInstructions() != null) target.setStorageInstructions(details.getStorageInstructions());
        if (details.getImageUrl() != null) target.setImageUrl(details.getImageUrl());
        return chemicalRepository.save(target);
    }

    @Transactional
    public void deleteChemical(Long id) {
        chemicalRepository.deleteById(id);
    }
}
