package com.vetsoftware.app.hospitalization.application.dto;

import com.vetsoftware.app.hospitalization.domain.ConsultationRef;
import java.time.LocalDate;

public record ConsultationSummaryDto(Long id, LocalDate date) {
    public static ConsultationSummaryDto from(ConsultationRef ref) {
        return new ConsultationSummaryDto(ref.id(), ref.date());
    }
}
