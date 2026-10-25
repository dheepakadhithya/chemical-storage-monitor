package com.college.chemical_storage_monitor.repository;

import com.college.chemical_storage_monitor.model.Chemical;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChemicalRepository extends JpaRepository<Chemical, Long> {
    Optional<Chemical> findByActiveTrue();
    List<Chemical> findAllByOrderByActiveDescNameAsc();
}
