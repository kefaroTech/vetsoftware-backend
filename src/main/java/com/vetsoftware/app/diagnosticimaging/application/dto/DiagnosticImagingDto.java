package com.vetsoftware.app.diagnosticimaging.application.dto;

import com.vetsoftware.app.diagnosticimaging.domain.DiagnosticImaging;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record DiagnosticImagingDto(
    Long id,
    LocalDate date,
    DiagnosticImagingTypeSummaryDto diagnosticImagingType,
    String clinicalSigns,
    String studyType,
    String diagnosis,
    String observations,
    String status,
    AnimalSummaryDto animal,
    ConsultationSummaryDto consultation,
    CompanySummaryDto company,
    LocalDateTime createdDate,
    boolean enabled) {
  public static DiagnosticImagingDto from(DiagnosticImaging imaging) {
    return new DiagnosticImagingDto(
        imaging.getId(),
        imaging.getDate(),
        DiagnosticImagingTypeSummaryDto.from(imaging.getDiagnosticImagingType()),
        imaging.getClinicalSigns(),
        imaging.getStudyType(),
        imaging.getDiagnosis(),
        imaging.getObservations(),
        imaging.getStatus().name(),
        AnimalSummaryDto.from(imaging.getAnimal()),
        imaging.getConsultation() == null
            ? null
            : ConsultationSummaryDto.from(imaging.getConsultation()),
        CompanySummaryDto.from(imaging.getCompany()),
        imaging.getCreatedDate(),
        imaging.isEnabled());
  }
}
