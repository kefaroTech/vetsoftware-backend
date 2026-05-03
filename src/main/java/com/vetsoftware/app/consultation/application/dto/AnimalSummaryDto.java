package com.vetsoftware.app.consultation.application.dto;

import com.vetsoftware.app.consultation.domain.AnimalRef;

public record AnimalSummaryDto(Long id, String name, String code) {
    public static AnimalSummaryDto from(AnimalRef ref) {
        return new AnimalSummaryDto(ref.id(), ref.name(), ref.code());
    }
}
