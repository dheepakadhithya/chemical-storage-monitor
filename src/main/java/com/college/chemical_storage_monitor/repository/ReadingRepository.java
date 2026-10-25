package com.college.chemical_storage_monitor.repository;

import com.college.chemical_storage_monitor.model.Reading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReadingRepository extends JpaRepository<Reading, Long> {

    // Find the most recent reading overall
    Reading findTopByOrderByCreatedAtDesc();

    // Find the most recent reading for a specific chemical
    Reading findTopByChemicalIdOrderByCreatedAtDesc(Long chemicalId);

    // Find all readings with a specific status (e.g., "danger")
    List<Reading> findByStatusOrderByCreatedAtDesc(String status);

    // Find all readings with status in a list of danger statuses
    List<Reading> findByStatusInOrderByCreatedAtDesc(List<String> statuses);

    // Find all readings for a specific chemical with status
    List<Reading> findByChemicalIdAndStatusOrderByCreatedAtDesc(Long chemicalId, String status);

    // Find all readings for a specific chemical with status in a list of danger statuses
    List<Reading> findByChemicalIdAndStatusInOrderByCreatedAtDesc(Long chemicalId, List<String> statuses);

    // Find all saved history (both normal and danger) ordered by date
    List<Reading> findAllByOrderByCreatedAtDesc();

    // Find all history for a specific chemical ordered by date
    List<Reading> findByChemicalIdOrderByCreatedAtDesc(Long chemicalId);
}
