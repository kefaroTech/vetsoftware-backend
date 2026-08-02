package com.vetsoftware.app.daycare.application.command;

import com.vetsoftware.app.daycare.domain.DayCareType;
import java.time.LocalDate;

public record CreateDayCareCommand(
    LocalDate date,
    LocalDate startDate,
    LocalDate endDate,
    DayCareType type,
    String objects,
    String observations,
    Long animalId,
    Long companyId) {}
