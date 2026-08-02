package com.vetsoftware.app.medicament.application.dto;

import com.vetsoftware.app.medicament.domain.Medicament;
import java.time.LocalDateTime;

public record MedicamentDto(
    Long id,
    String name,
    String description,
    CompanySummaryDto company,
    boolean general,
    LocalDateTime createdDate,
    boolean enabled) {
  public static MedicamentDto from(Medicament medicament) {
    return new MedicamentDto(
        medicament.getId(),
        medicament.getName(),
        medicament.getDescription(),
        medicament.getCompany() == null ? null : CompanySummaryDto.from(medicament.getCompany()),
        medicament.isGeneral(),
        medicament.getCreatedDate(),
        medicament.isEnabled());
  }
}
