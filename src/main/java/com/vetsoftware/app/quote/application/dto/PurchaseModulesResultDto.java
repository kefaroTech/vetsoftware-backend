package com.vetsoftware.app.quote.application.dto;

import java.util.List;

public record PurchaseModulesResultDto(Long quoteId, List<PurchasedModuleLineDto> lines) {
}
