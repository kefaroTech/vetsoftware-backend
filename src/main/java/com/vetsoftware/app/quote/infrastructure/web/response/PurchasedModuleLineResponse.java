package com.vetsoftware.app.quote.infrastructure.web.response;

import com.vetsoftware.app.quote.application.dto.PurchasedModuleLineDto;
import java.time.LocalDate;

public record PurchasedModuleLineResponse(String catalogItemCode, LocalDate firstChargeDate,
        boolean chargedNow) {

    public static PurchasedModuleLineResponse from(PurchasedModuleLineDto dto) {
        return new PurchasedModuleLineResponse(dto.catalogItemCode(), dto.firstChargeDate(),
                dto.chargedNow());
    }
}
