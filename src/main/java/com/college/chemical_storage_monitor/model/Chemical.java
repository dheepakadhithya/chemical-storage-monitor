package com.college.chemical_storage_monitor.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "chemicals")
public class Chemical {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String formula;
    private String hazardCategory;
    private Double maxTemperature; // Max safe temperature in °C
    private Double maxHumidity;    // Max safe relative humidity in %
    private String storageInstructions;

    @Column(length = 1000)
    private String imageUrl;       // Image URL for chemical bottle / hazard visualization

    private boolean active = false; // Whether this is the active chemical being monitored

    public Chemical() {
    }

    public Chemical(String name, String formula, String hazardCategory, 
                    Double maxTemperature, Double maxHumidity, 
                    String storageInstructions, boolean active) {
        this(name, formula, hazardCategory, maxTemperature, maxHumidity, storageInstructions, null, active);
    }

    public Chemical(String name, String formula, String hazardCategory, 
                    Double maxTemperature, Double maxHumidity, 
                    String storageInstructions, String imageUrl, boolean active) {
        this.name = name;
        this.formula = formula;
        this.hazardCategory = hazardCategory;
        this.maxTemperature = maxTemperature;
        this.maxHumidity = maxHumidity;
        this.storageInstructions = storageInstructions;
        this.imageUrl = imageUrl;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFormula() {
        return formula;
    }

    public void setFormula(String formula) {
        this.formula = formula;
    }

    public String getHazardCategory() {
        return hazardCategory;
    }

    public void setHazardCategory(String hazardCategory) {
        this.hazardCategory = hazardCategory;
    }

    public Double getMaxTemperature() {
        return maxTemperature;
    }

    public void setMaxTemperature(Double maxTemperature) {
        this.maxTemperature = maxTemperature;
    }

    public Double getMaxHumidity() {
        return maxHumidity;
    }

    public void setMaxHumidity(Double maxHumidity) {
        this.maxHumidity = maxHumidity;
    }

    public String getStorageInstructions() {
        return storageInstructions;
    }

    public void setStorageInstructions(String storageInstructions) {
        this.storageInstructions = storageInstructions;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
