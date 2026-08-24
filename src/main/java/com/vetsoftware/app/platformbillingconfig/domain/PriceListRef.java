package com.vetsoftware.app.platformbillingconfig.domain;

/**
 * Companion VO de la tarifa por defecto de la plataforma (FK a la feature
 * {@code pricelist}). Lleva solo lo que esta feature necesita mostrar en la
 * consola: el código con el que se identifica la lista y su nombre legible.
 *
 * <p>
 * Existe para que este slice no importe el dominio de {@code pricelist}: la
 * única frontera que se cruza es la de {@code infrastructure/persistence}, en
 * {@code JpaPriceListQueryPort}.
 */
public record PriceListRef(Long id, String code, String name) {
    public PriceListRef {
        if (id == null)
            throw new IllegalArgumentException("price list id is required");
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("price list code is required");
        if (name == null || name.isBlank())
            throw new IllegalArgumentException("price list name is required");
    }
}
