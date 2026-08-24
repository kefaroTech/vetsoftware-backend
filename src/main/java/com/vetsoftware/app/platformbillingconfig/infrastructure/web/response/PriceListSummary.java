package com.vetsoftware.app.platformbillingconfig.infrastructure.web.response;

/**
 * Forma JSON de la tarifa por defecto. Es propia de este slice: nunca se expone
 * la {@code PriceListResponse} de la feature {@code pricelist}.
 */
public record PriceListSummary(Long id, String code, String name) {
}
