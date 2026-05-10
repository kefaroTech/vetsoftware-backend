package com.vetsoftware.app.laboratorytest.application.dto;

import com.vetsoftware.app.laboratorytest.domain.LaboratoryTest;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record LaboratoryTestDto(
        Long id,
        LocalDate date,
        LaboratoryTestTypeSummaryDto testType,
        Integer quantity,
        String diagnosis,
        AnimalSummaryDto animal,
        ConsultationSummaryDto consultation,
        CompanySummaryDto company,
        LocalDateTime createdDate
) {
    public static LaboratoryTestDto from(LaboratoryTest laboratoryTest) {
        return new LaboratoryTestDto(
            laboratoryTest.getId(),
            laboratoryTest.getDate(),
            LaboratoryTestTypeSummaryDto.from(laboratoryTest.getTestType()),
            laboratoryTest.getQuantity(),
            laboratoryTest.getDiagnosis(),
            AnimalSummaryDto.from(laboratoryTest.getAnimal()),
            laboratoryTest.getConsultation() == null ? null : ConsultationSummaryDto.from(laboratoryTest.getConsultation()),
            CompanySummaryDto.from(laboratoryTest.getCompany()),
            laboratoryTest.getCreatedDate()
        );
    }
}
