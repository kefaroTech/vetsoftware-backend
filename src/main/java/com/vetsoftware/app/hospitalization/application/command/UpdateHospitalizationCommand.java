package com.vetsoftware.app.hospitalization.application.command;

import com.vetsoftware.app.hospitalization.domain.HospitalizationType;
import com.vetsoftware.app.hospitalization.domain.ReasonLeaving;
import java.time.LocalDate;

public record UpdateHospitalizationCommand(
    Long id,
    LocalDate date,
    LocalDate startDate,
    LocalDate endDate,
    HospitalizationType type,
    ReasonLeaving reasonLeaving,
    String reason,
    String observations,
    Long animalId,
    Long consultationId,
    Long companyId) {}
