package com.vetsoftware.app.hospitalizationobservation.application.dto;

import com.vetsoftware.app.hospitalizationobservation.domain.HospitalizationObservation;
import java.time.LocalDateTime;

public record HospitalizationObservationDto(
    Long id,
    String description,
    HospitalizationSummaryDto hospitalization,
    EmployeeSummaryDto createdBy,
    LocalDateTime createdDate,
    boolean enabled) {
  public static HospitalizationObservationDto from(HospitalizationObservation observation) {
    return new HospitalizationObservationDto(
        observation.getId(),
        observation.getDescription(),
        HospitalizationSummaryDto.from(observation.getHospitalization()),
        EmployeeSummaryDto.from(observation.getCreatedBy()),
        observation.getCreatedDate(),
        observation.isEnabled());
  }
}
