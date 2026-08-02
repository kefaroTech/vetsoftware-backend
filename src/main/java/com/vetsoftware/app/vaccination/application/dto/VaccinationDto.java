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
    String route,
    String applicationSite,
    LocalDate nextVaccination,
    AnimalSummaryDto animal,
    ConsultationSummaryDto consultation,
    CompanySummaryDto company,
    LocalDateTime createdDate,
    boolean enabled) {
  public static VaccinationDto from(Vaccination vaccination) {
    return new VaccinationDto(
        vaccination.getId(),
        vaccination.getDate(),
        VaccinationTypeSummaryDto.from(vaccination.getVaccinationType()),
        vaccination.getLot(),
        vaccination.getNotes(),
        vaccination.getRoute(),
        vaccination.getApplicationSite(),
        vaccination.getNextVaccination(),
        AnimalSummaryDto.from(vaccination.getAnimal()),
        vaccination.getConsultation() == null
            ? null
            : ConsultationSummaryDto.from(vaccination.getConsultation()),
        CompanySummaryDto.from(vaccination.getCompany()),
        vaccination.getCreatedDate(),
        vaccination.isEnabled());
  }
}
