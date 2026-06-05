package com.vetsoftware.app.productchargeopenaccount.application.dto;

import com.vetsoftware.app.productchargeopenaccount.domain.AnimalRef;

public record AnimalSummaryDto(Long id, String name, String code) {
    public static AnimalSummaryDto from(AnimalRef animal) {
        return new AnimalSummaryDto(animal.id(), animal.name(), animal.code());
    }
}
