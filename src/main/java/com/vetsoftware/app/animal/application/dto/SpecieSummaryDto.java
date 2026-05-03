package com.vetsoftware.app.animal.application.dto;

import com.vetsoftware.app.animal.domain.SpecieRef;

public record SpecieSummaryDto(Long id, String name) {
    public static SpecieSummaryDto from(SpecieRef ref) {
        return new SpecieSummaryDto(ref.id(), ref.name());
    }
}
