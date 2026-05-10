package com.vetsoftware.app.surgery.application.dto;

import com.vetsoftware.app.surgery.domain.ConsultationRef;
import java.time.LocalDate;

public record ConsultationSummaryDto(Long id, LocalDate date) {
    public static ConsultationSummaryDto from(ConsultationRef ref) {
        return new ConsultationSummaryDto(ref.id(), ref.date());
    }
}
