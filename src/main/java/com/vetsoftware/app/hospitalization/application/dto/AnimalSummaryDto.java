package com.vetsoftware.app.hospitalization.application.dto;

import com.vetsoftware.app.hospitalization.domain.AnimalRef;

public record AnimalSummaryDto(Long id, String name, String code) {
    public static AnimalSummaryDto from(AnimalRef ref) {
        return new AnimalSummaryDto(ref.id(), ref.name(), ref.code());
    }
}
