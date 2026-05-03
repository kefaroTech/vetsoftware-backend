package com.vetsoftware.app.vaccination.application.dto;

import com.vetsoftware.app.vaccination.domain.VaccinationTypeRef;

public record VaccinationTypeSummaryDto(Long id, String name) {
    public static VaccinationTypeSummaryDto from(VaccinationTypeRef ref) {
        return new VaccinationTypeSummaryDto(ref.id(), ref.name());
    }
}
