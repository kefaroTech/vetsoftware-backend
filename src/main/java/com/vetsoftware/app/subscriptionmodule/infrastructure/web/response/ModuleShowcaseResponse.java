package com.vetsoftware.app.subscriptionmodule.infrastructure.web.response;

import com.vetsoftware.app.subscriptionmodule.application.dto.ModuleShowcaseDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ModuleShowcaseResponse(String code, String name, String shortDescription,
        String state, LocalDate trialEndDate, List<ModuleCeilingResponse> ceilings,
        BigDecimal monthlyPrice, BigDecimal annualPrice, boolean purchasable, boolean canPurchase) {

    public static ModuleShowcaseResponse from(ModuleShowcaseDto dto) {
        return new ModuleShowcaseResponse(dto.code(), dto.name(), dto.shortDescription(),
                dto.state().name(), dto.trialEndDate(),
                dto.ceilings().stream().map(ModuleCeilingResponse::from).toList(),
                dto.monthlyPrice(), dto.annualPrice(), dto.purchasable(), dto.canPurchase());
    }
}
