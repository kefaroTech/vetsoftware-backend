package com.vetsoftware.app.platformbillingconfig.application.dto;

import com.vetsoftware.app.platformbillingconfig.domain.PriceListRef;

/** Vista de salida de la tarifa por defecto, sin invariantes. */
public record PriceListSummaryDto(Long id, String code, String name) {

    public static PriceListSummaryDto from(PriceListRef ref) {
        return ref == null ? null : new PriceListSummaryDto(ref.id(), ref.code(), ref.name());
    }
}
