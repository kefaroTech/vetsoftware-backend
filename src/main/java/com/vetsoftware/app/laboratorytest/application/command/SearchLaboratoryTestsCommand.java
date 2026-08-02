package com.vetsoftware.app.laboratorytest.application.command;

import com.vetsoftware.app.laboratorytest.domain.LaboratoryTestPriority;
import com.vetsoftware.app.laboratorytest.domain.LaboratoryTestStatus;
import java.time.LocalDate;
import java.util.List;

public record SearchLaboratoryTestsCommand(
    Long companyId,
    Long branchId,
    List<LaboratoryTestStatus> statuses,
    Long animalId,
    Long testTypeId,
    LaboratoryTestPriority prioridad,
    LocalDate dateFrom,
    LocalDate dateTo,
    int page,
    int pageSize) {}
