package com.college.chemical_storage_monitor.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import java.time.LocalDateTime;

@Entity
public class Reading {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Double temperature;
    private Double humidity;
    
    // "safe" or "danger"
    private String status;

    private String chemicalName;
    private Long chemicalId;
    private Double tempThreshold;
    private Double humidityThreshold;
    private String alertReason;

    // Ultrasonic radar & Interruption Security fields
    private Double distance;
    private Double distanceThreshold;
    private Boolean interruption;
    private String dangerType;

    private LocalDateTime createdAt;

    @Transient
    private String fanControl;

    @Transient
    private String buzzerControl;

    @Transient
    private String lightControl;

    @Transient
    private Integer lightBrightness;

    @Transient
    private Integer buzzerVolume;

    @Transient
    private String buzzerPattern;

    @Transient
    private Integer buzzerTone;

    @Transient
    private String cabinetLock;

    public Reading() {
        this.createdAt = LocalDateTime.now();
    }

    public Reading(Double temperature, Double humidity, String status) {
        this.temperature = temperature;
        this.humidity = humidity;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Double getHumidity() {
        return humidity;
    }

    public void setHumidity(Double humidity) {
        this.humidity = humidity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getChemicalId() {
        return chemicalId;
    }

    public void setChemicalId(Long chemicalId) {
        this.chemicalId = chemicalId;
    }

    public String getChemicalName() {
        return chemicalName;
    }

    public void setChemicalName(String chemicalName) {
        this.chemicalName = chemicalName;
    }

    public Double getTempThreshold() {
        return tempThreshold;
    }

    public void setTempThreshold(Double tempThreshold) {
        this.tempThreshold = tempThreshold;
    }

    public Double getHumidityThreshold() {
        return humidityThreshold;
    }

    public void setHumidityThreshold(Double humidityThreshold) {
        this.humidityThreshold = humidityThreshold;
    }

    public String getAlertReason() {
        return alertReason;
    }

    public void setAlertReason(String alertReason) {
        this.alertReason = alertReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Transient
    public String getTemperatureStatus() {
        if (temperature == null) {
            return null;
        }
        double limit = (tempThreshold != null) ? tempThreshold : 50.00;
        return temperature >= limit ? "danger" : "safe";
    }

    @Transient
    public String getHumidityStatus() {
        if (humidity == null) {
            return null;
        }
        double limit = (humidityThreshold != null) ? humidityThreshold : 85.00;
        return humidity > limit ? "danger" : "safe";
    }

    public String getFanControl() {
        return fanControl;
    }

    public void setFanControl(String fanControl) {
        this.fanControl = fanControl;
    }

    public String getBuzzerControl() {
        return buzzerControl;
    }

    public void setBuzzerControl(String buzzerControl) {
        this.buzzerControl = buzzerControl;
    }

    public String getLightControl() {
        return lightControl;
    }

    public void setLightControl(String lightControl) {
        this.lightControl = lightControl;
    }

    public Integer getLightBrightness() {
        return lightBrightness;
    }

    public void setLightBrightness(Integer lightBrightness) {
        this.lightBrightness = lightBrightness;
    }

    public Integer getBuzzerVolume() {
        return buzzerVolume;
    }

    public void setBuzzerVolume(Integer buzzerVolume) {
        this.buzzerVolume = buzzerVolume;
    }

    public String getBuzzerPattern() {
        return buzzerPattern;
    }

    public void setBuzzerPattern(String buzzerPattern) {
        this.buzzerPattern = buzzerPattern;
    }

    public Integer getBuzzerTone() {
        return buzzerTone;
    }

    public void setBuzzerTone(Integer buzzerTone) {
        this.buzzerTone = buzzerTone;
    }

    public Double getDistance() {
        return distance;
    }

    public void setDistance(Double distance) {
        this.distance = distance;
    }

    public Double getDistanceThreshold() {
        return distanceThreshold;
    }

    public void setDistanceThreshold(Double distanceThreshold) {
        this.distanceThreshold = distanceThreshold;
    }

    public Boolean getInterruption() {
        return interruption;
    }

    public void setInterruption(Boolean interruption) {
        this.interruption = interruption;
    }

    public String getDangerType() {
        return dangerType;
    }

    public void setDangerType(String dangerType) {
        this.dangerType = dangerType;
    }

    public String getCabinetLock() {
        return cabinetLock;
    }

    public void setCabinetLock(String cabinetLock) {
        this.cabinetLock = cabinetLock;
    }
}
