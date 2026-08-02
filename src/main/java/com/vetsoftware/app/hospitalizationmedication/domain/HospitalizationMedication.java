package com.vetsoftware.app.hospitalizationmedication.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class HospitalizationMedication {
    private Long id;
    private String name;
    private String dose;
    private Frequency frequency;
    private GuidelineType guidelineType;
    private DurationMeasure durationMeasure;
    private Integer durationQuantity;
    private LocalDate startDate;
    private LocalTime startTime;
    private String notes;
    private HospitalizationRef hospitalization;
    private final EmployeeRef createdBy;
    private final LocalDateTime createdDate;
    private boolean enabled;
    private LocalDateTime suspensionDate;
    private EmployeeRef suspensionBy;

    public HospitalizationMedication(Long id, String name, String dose, Frequency frequency,
            GuidelineType guidelineType, DurationMeasure durationMeasure, Integer durationQuantity,
            LocalDate startDate, LocalTime startTime, String notes,
            HospitalizationRef hospitalization, EmployeeRef createdBy, LocalDateTime createdDate,
            boolean enabled, LocalDateTime suspensionDate, EmployeeRef suspensionBy) {
        validate(name, hospitalization, createdBy);
        this.id = id;
        this.name = name;
        this.dose = dose;
        this.frequency = frequency;
        this.guidelineType = guidelineType;
        this.durationMeasure = durationMeasure;
        this.durationQuantity = durationQuantity;
        this.startDate = startDate;
        this.startTime = startTime;
        this.notes = notes;
        this.hospitalization = hospitalization;
        this.createdBy = createdBy;
        this.createdDate = createdDate;
        this.enabled = enabled;
        this.suspensionDate = suspensionDate;
        this.suspensionBy = suspensionBy;
    }

    public static HospitalizationMedication create(String name, String dose, Frequency frequency,
            GuidelineType guidelineType, DurationMeasure durationMeasure, Integer durationQuantity,
            LocalDate startDate, LocalTime startTime, String notes,
            HospitalizationRef hospitalization, EmployeeRef createdBy) {
        return new HospitalizationMedication(null, name, dose, frequency, guidelineType,
                durationMeasure, durationQuantity, startDate, startTime, notes, hospitalization,
                createdBy, LocalDateTime.now(), true, null, null);
    }

    /**
     * Suspende la orden: registra quién y cuándo. Las dosis aplicadas no se tocan.
     */
    public void suspend(EmployeeRef by, LocalDateTime when) {
        if (by == null)
            throw new IllegalArgumentException("suspensionBy is required");
        if (when == null)
            throw new IllegalArgumentException("suspensionDate is required");
        this.suspensionDate = when;
        this.suspensionBy = by;
    }

    public void update(String name, String dose, Frequency frequency, GuidelineType guidelineType,
            DurationMeasure durationMeasure, Integer durationQuantity, LocalDate startDate,
            LocalTime startTime, String notes) {
        validate(name, this.hospitalization, this.createdBy);
        this.name = name;
        this.dose = dose;
        this.frequency = frequency;
        this.guidelineType = guidelineType;
        this.durationMeasure = durationMeasure;
        this.durationQuantity = durationQuantity;
        this.startDate = startDate;
        this.startTime = startTime;
        this.notes = notes;
    }

    public void enable() {
        this.enabled = true;
    }

    public void disable() {
        this.enabled = false;
    }

    private static void validate(String name, HospitalizationRef hospitalization,
            EmployeeRef createdBy) {
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("name is required");
        if (name.length() > 200)
            throw new IllegalArgumentException("name must be 200 chars or less");
        if (hospitalization == null)
            throw new IllegalArgumentException("hospitalization is required");
        if (createdBy == null)
            throw new IllegalArgumentException("createdBy is required");
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDose() {
        return dose;
    }

    public Frequency getFrequency() {
        return frequency;
    }

    public GuidelineType getGuidelineType() {
        return guidelineType;
    }

    public DurationMeasure getDurationMeasure() {
        return durationMeasure;
    }

    public Integer getDurationQuantity() {
        return durationQuantity;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public String getNotes() {
        return notes;
    }

    public HospitalizationRef getHospitalization() {
        return hospitalization;
    }

    public EmployeeRef getCreatedBy() {
        return createdBy;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public LocalDateTime getSuspensionDate() {
        return suspensionDate;
    }

    public EmployeeRef getSuspensionBy() {
        return suspensionBy;
    }
}
