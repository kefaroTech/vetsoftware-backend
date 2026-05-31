package com.vetsoftware.app.hospitalizationmedication.application.dto;

import com.vetsoftware.app.hospitalizationmedication.domain.HospitalizationRef;
import java.time.LocalDate;

public record HospitalizationSummaryDto(Long id, LocalDate date) {
    public static HospitalizationSummaryDto from(HospitalizationRef ref) {
        return new HospitalizationSummaryDto(ref.id(), ref.date());
    }
}
