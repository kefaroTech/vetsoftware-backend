package com.vetsoftware.app.laboratorytest.application.dto;

import com.vetsoftware.app.laboratorytest.domain.LaboratoryTest;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record LaboratoryTestDto(
        Long id,
        LocalDate date,
        TestTypeSummaryDto testType,
        Integer quantity,
        String diagnosis,
        AnimalSummaryDto animal,
        CompanySummaryDto company,
        LocalDateTime createdDate
) {
    public static LaboratoryTestDto from(LaboratoryTest laboratoryTest) {
        return new LaboratoryTestDto(
            laboratoryTest.getId(),
            laboratoryTest.getDate(),
            TestTypeSummaryDto.from(laboratoryTest.getTestType()),
            laboratoryTest.getQuantity(),
            laboratoryTest.getDiagnosis(),
            AnimalSummaryDto.from(laboratoryTest.getAnimal()),
            CompanySummaryDto.from(laboratoryTest.getCompany()),
            laboratoryTest.getCreatedDate()
        );
    }
}
