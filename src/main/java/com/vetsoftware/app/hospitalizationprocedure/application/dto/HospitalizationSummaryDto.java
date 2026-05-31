package com.vetsoftware.app.hospitalizationprocedure.application.dto;

import com.vetsoftware.app.hospitalizationprocedure.domain.HospitalizationRef;
import java.time.LocalDate;

public record HospitalizationSummaryDto(Long id, LocalDate date) {
    public static HospitalizationSummaryDto from(HospitalizationRef ref) {
        return new HospitalizationSummaryDto(ref.id(), ref.date());
    }
}
