package com.vetsoftware.app.laboratorytest.application.dto;

import com.vetsoftware.app.laboratorytest.domain.LaboratoryTest;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record LaboratoryTestDto(Long id, LocalDate date, LaboratoryTestTypeSummaryDto testType,
        Integer quantity, String diagnosis, String status, String prioridad,
        AnimalSummaryDto animal, ConsultationSummaryDto consultation, CompanySummaryDto company,
        EmployeeSummaryDto processedBy, LocalDateTime processedDate, LocalDateTime createdDate,
        boolean enabled) {
    public static LaboratoryTestDto from(LaboratoryTest laboratoryTest) {
        return new LaboratoryTestDto(laboratoryTest.getId(), laboratoryTest.getDate(),
                LaboratoryTestTypeSummaryDto.from(laboratoryTest.getTestType()),
                laboratoryTest.getQuantity(), laboratoryTest.getDiagnosis(),
                laboratoryTest.getStatus().name(), laboratoryTest.getPrioridad().name(),
                AnimalSummaryDto.from(laboratoryTest.getAnimal()),
                laboratoryTest.getConsultation() == null
                        ? null
                        : ConsultationSummaryDto.from(laboratoryTest.getConsultation()),
                CompanySummaryDto.from(laboratoryTest.getCompany()),
                laboratoryTest.getProcessedBy() == null
                        ? null
                        : EmployeeSummaryDto.from(laboratoryTest.getProcessedBy()),
                laboratoryTest.getProcessedDate(), laboratoryTest.getCreatedDate(),
                laboratoryTest.isEnabled());
    }
}
