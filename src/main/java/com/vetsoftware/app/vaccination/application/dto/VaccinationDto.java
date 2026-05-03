package com.vetsoftware.app.vaccination.application.dto;

import com.vetsoftware.app.vaccination.domain.Vaccination;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record VaccinationDto(
        Long id,
        LocalDate date,
        VaccinationTypeSummaryDto vaccinationType,
        String lot,
        String notes,
        LocalDate nextVaccination,
        AnimalSummaryDto animal,
        CompanySummaryDto company,
        LocalDateTime createdDate
) {
    public static VaccinationDto from(Vaccination vaccination) {
        return new VaccinationDto(
            vaccination.getId(),
            vaccination.getDate(),
            VaccinationTypeSummaryDto.from(vaccination.getVaccinationType()),
            vaccination.getLot(),
            vaccination.getNotes(),
            vaccination.getNextVaccination(),
            AnimalSummaryDto.from(vaccination.getAnimal()),
            CompanySummaryDto.from(vaccination.getCompany()),
            vaccination.getCreatedDate()
        );
    }
}
