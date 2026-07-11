package com.vetsoftware.app.branch.application.dto;

import com.vetsoftware.app.branch.domain.CityRef;

public record CitySummaryDto(Long id, String name) {
    public static CitySummaryDto from(CityRef ref) {
        return new CitySummaryDto(ref.id(), ref.name());
    }
}
