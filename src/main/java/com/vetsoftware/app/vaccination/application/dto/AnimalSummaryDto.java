package com.vetsoftware.app.vaccination.application.dto;

import com.vetsoftware.app.vaccination.domain.AnimalRef;

public record AnimalSummaryDto(Long id, String name, String code) {
    public static AnimalSummaryDto from(AnimalRef ref) {
        return new AnimalSummaryDto(ref.id(), ref.name(), ref.code());
    }
}
