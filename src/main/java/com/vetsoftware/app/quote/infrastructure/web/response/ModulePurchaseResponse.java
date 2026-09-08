package com.vetsoftware.app.quote.infrastructure.web.response;

import com.vetsoftware.app.quote.application.dto.PurchaseModulesResultDto;
import java.util.List;

public record ModulePurchaseResponse(Long quoteId, List<PurchasedModuleLineResponse> lines) {

    public static ModulePurchaseResponse from(PurchaseModulesResultDto dto) {
        return new ModulePurchaseResponse(dto.quoteId(),
                dto.lines().stream().map(PurchasedModuleLineResponse::from).toList());
    }
}
