package com.vetsoftware.app.animalcolor.application.dto;

import com.vetsoftware.app.animalcolor.domain.SpecieRef;

public record SpecieSummaryDto(Long id, String name) {
    public static SpecieSummaryDto from(SpecieRef ref) {
        return new SpecieSummaryDto(ref.id(), ref.name());
    }
}
