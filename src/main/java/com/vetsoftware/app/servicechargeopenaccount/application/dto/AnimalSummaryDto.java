package com.vetsoftware.app.servicechargeopenaccount.application.dto;

import com.vetsoftware.app.servicechargeopenaccount.domain.AnimalRef;

public record AnimalSummaryDto(Long id, String name, String code) {
    public static AnimalSummaryDto from(AnimalRef animal) {
        return new AnimalSummaryDto(animal.id(), animal.name(), animal.code());
    }
}
