package com.vetsoftware.app.quote.domain;

/** Companion VO de la tarifa con la que se cotizo, congelada en la cabecera. */
public record PriceListRef(Long id, String code, String currency) {
    public PriceListRef {
        if (id == null)
            throw new IllegalArgumentException("price list id is required");
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("price list code is required");
        if (currency == null || currency.isBlank())
            throw new IllegalArgumentException("price list currency is required");
    }
}
