package com.vetsoftware.app.subscription.application.port.out;

/**
 * Valida que la tarifa con la que se firma existe. {@code price_lists} es
 * global de plataforma y no lleva {@code company_id}.
 */
public interface PriceListValidationPort {
    void validateExists(Long priceListId);
}
