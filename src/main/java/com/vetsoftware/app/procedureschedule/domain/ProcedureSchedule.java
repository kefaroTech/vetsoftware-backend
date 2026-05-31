package com.vetsoftware.app.procedureschedule.domain;

import java.time.LocalDateTime;

public class ProcedureSchedule {
    private Long id;
    private HospitalizationProcedureRef hospitalizationProcedure;
    private LocalDateTime originalDateTime;
    private LocalDateTime currentDateTime;
    private LocalDateTime realDateTime;
    private AppliedStatus appliedStatus;
    private Boolean rescheduled;
    private final EmployeeRef createdBy;
    private final LocalDateTime createdDate;
    private boolean enabled;

    public ProcedureSchedule(Long id, HospitalizationProcedureRef hospitalizationProcedure,
                             LocalDateTime originalDateTime, LocalDateTime currentDateTime,
                             LocalDateTime realDateTime, AppliedStatus appliedStatus, Boolean rescheduled,
                             EmployeeRef createdBy, LocalDateTime createdDate, boolean enabled) {
        validate(hospitalizationProcedure, originalDateTime, createdBy);
        this.id = id;
        this.hospitalizationProcedure = hospitalizationProcedure;
        this.originalDateTime = originalDateTime;
        this.currentDateTime = currentDateTime;
        this.realDateTime = realDateTime;
        this.appliedStatus = appliedStatus;
        this.rescheduled = rescheduled;
        this.createdBy = createdBy;
        this.createdDate = createdDate;
        this.enabled = enabled;
    }

    public static ProcedureSchedule create(HospitalizationProcedureRef hospitalizationProcedure,
                                           LocalDateTime originalDateTime, LocalDateTime currentDateTime,
                                           AppliedStatus appliedStatus, Boolean rescheduled,
                                           EmployeeRef createdBy) {
        return new ProcedureSchedule(null, hospitalizationProcedure, originalDateTime, currentDateTime,
            null, appliedStatus, rescheduled, createdBy, LocalDateTime.now(), true);
    }

    public void enable() { this.enabled = true; }

    public void disable() { this.enabled = false; }

    private static void validate(HospitalizationProcedureRef hospitalizationProcedure,
                                 LocalDateTime originalDateTime, EmployeeRef createdBy) {
        if (hospitalizationProcedure == null)
            throw new IllegalArgumentException("hospitalizationProcedure is required");
        if (createdBy == null) throw new IllegalArgumentException("createdBy is required");
        if (originalDateTime == null) throw new IllegalArgumentException("originalDateTime is required");
    }

    public Long getId() { return id; }
    public HospitalizationProcedureRef getHospitalizationProcedure() { return hospitalizationProcedure; }
    public LocalDateTime getOriginalDateTime() { return originalDateTime; }
    public LocalDateTime getCurrentDateTime() { return currentDateTime; }
    public LocalDateTime getRealDateTime() { return realDateTime; }
    public AppliedStatus getAppliedStatus() { return appliedStatus; }
    public Boolean getRescheduled() { return rescheduled; }
    public EmployeeRef getCreatedBy() { return createdBy; }
    public LocalDateTime getCreatedDate() { return createdDate; }
    public boolean isEnabled() { return enabled; }
}
