package com.vetsoftware.app.companytaxprofile.application.dto;

import com.vetsoftware.app.companytaxprofile.domain.EconomicActivityRef;

public record EconomicActivitySummaryDto(Long id, String code, String name) {
    public static EconomicActivitySummaryDto from(EconomicActivityRef ref) {
        return new EconomicActivitySummaryDto(ref.id(), ref.code(), ref.name());
    }
}
