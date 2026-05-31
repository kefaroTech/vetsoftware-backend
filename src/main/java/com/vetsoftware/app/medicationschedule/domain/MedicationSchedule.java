package com.vetsoftware.app.medicationschedule.domain;

import java.time.LocalDateTime;

public class MedicationSchedule {
    private Long id;
    private HospitalizationMedicationRef hospitalizationMedication;
    private LocalDateTime originalDateTime;
    private LocalDateTime currentDateTime;
    private LocalDateTime realDateTime;
    private AppliedStatus appliedStatus;
    private Boolean rescheduled;
    private final EmployeeRef createdBy;
    private final LocalDateTime createdDate;
    private boolean enabled;

    public MedicationSchedule(Long id, HospitalizationMedicationRef hospitalizationMedication,
                              LocalDateTime originalDateTime, LocalDateTime currentDateTime,
                              LocalDateTime realDateTime, AppliedStatus appliedStatus, Boolean rescheduled,
                              EmployeeRef createdBy, LocalDateTime createdDate, boolean enabled) {
        validate(hospitalizationMedication, originalDateTime, createdBy);
        this.id = id;
        this.hospitalizationMedication = hospitalizationMedication;
        this.originalDateTime = originalDateTime;
        this.currentDateTime = currentDateTime;
        this.realDateTime = realDateTime;
        this.appliedStatus = appliedStatus;
        this.rescheduled = rescheduled;
        this.createdBy = createdBy;
        this.createdDate = createdDate;
        this.enabled = enabled;
    }

    public static MedicationSchedule create(HospitalizationMedicationRef hospitalizationMedication,
                                            LocalDateTime originalDateTime, LocalDateTime currentDateTime,
                                            AppliedStatus appliedStatus, Boolean rescheduled,
                                            EmployeeRef createdBy) {
        return new MedicationSchedule(null, hospitalizationMedication, originalDateTime, currentDateTime,
            null, appliedStatus, rescheduled, createdBy, LocalDateTime.now(), true);
    }

    public void enable() { this.enabled = true; }

    public void disable() { this.enabled = false; }

    /** Marca la toma como aplicada en la hora real indicada. */
    public void apply(LocalDateTime realDateTime) {
        if (realDateTime == null) throw new IllegalArgumentException("realDateTime is required");
        this.appliedStatus = AppliedStatus.APPLIED;
        this.realDateTime = realDateTime;
    }

    /** Reprograma la toma a una nueva hora (marca rescheduled). */
    public void reschedule(LocalDateTime newCurrentDateTime) {
        if (newCurrentDateTime == null) throw new IllegalArgumentException("currentDateTime is required");
        this.currentDateTime = newCurrentDateTime;
        this.rescheduled = true;
    }

    private static void validate(HospitalizationMedicationRef hospitalizationMedication,
                                 LocalDateTime originalDateTime, EmployeeRef createdBy) {
        if (hospitalizationMedication == null)
            throw new IllegalArgumentException("hospitalizationMedication is required");
        if (createdBy == null) throw new IllegalArgumentException("createdBy is required");
        if (originalDateTime == null) throw new IllegalArgumentException("originalDateTime is required");
    }

    public Long getId() { return id; }
    public HospitalizationMedicationRef getHospitalizationMedication() { return hospitalizationMedication; }
    public LocalDateTime getOriginalDateTime() { return originalDateTime; }
    public LocalDateTime getCurrentDateTime() { return currentDateTime; }
    public LocalDateTime getRealDateTime() { return realDateTime; }
    public AppliedStatus getAppliedStatus() { return appliedStatus; }
    public Boolean getRescheduled() { return rescheduled; }
    public EmployeeRef getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public boolean isEnabled() { return enabled; }
}
