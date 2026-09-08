package com.vetsoftware.app.subscriptionmodule.application.dto;

import com.vetsoftware.app.subscriptionmodule.domain.ModuleState;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ModuleShowcaseDto(String code, String name, String shortDescription,
        ModuleState state, LocalDate trialEndDate, List<ModuleCeilingDto> ceilings,
        BigDecimal monthlyPrice, BigDecimal annualPrice, boolean purchasable, boolean canPurchase) {
}
