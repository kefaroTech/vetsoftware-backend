package com.vetsoftware.app.hospitalization.application.dto;

import com.vetsoftware.app.hospitalization.domain.Hospitalization;
import com.vetsoftware.app.hospitalization.domain.HospitalizationType;
import com.vetsoftware.app.hospitalization.domain.ReasonLeaving;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record HospitalizationDto(Long id, LocalDate date, LocalDate startDate, LocalDate endDate,
        HospitalizationType type, ReasonLeaving reasonLeaving, String reason, String observations,
        AnimalSummaryDto animal, ConsultationSummaryDto consultation, CompanySummaryDto company,
        LocalDateTime createdDate, boolean enabled) {
    public static HospitalizationDto from(Hospitalization hospitalization) {
        return new HospitalizationDto(hospitalization.getId(), hospitalization.getDate(),
                hospitalization.getStartDate(), hospitalization.getEndDate(),
                hospitalization.getType(), hospitalization.getReasonLeaving(),
                hospitalization.getReason(), hospitalization.getObservations(),
                AnimalSummaryDto.from(hospitalization.getAnimal()),
                hospitalization.getConsultation() == null
                        ? null
                        : ConsultationSummaryDto.from(hospitalization.getConsultation()),
                CompanySummaryDto.from(hospitalization.getCompany()),
                hospitalization.getCreatedDate(), hospitalization.isEnabled());
    }
}
