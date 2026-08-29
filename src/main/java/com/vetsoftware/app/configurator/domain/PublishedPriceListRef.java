package com.vetsoftware.app.configurator.domain;

import java.time.LocalDate;

/**
 * Una tarifa publicada, vista por el configurador: solo lo que hace falta para
 * decidir cual rige hoy.
 *
 * <p>
 * Ni moneda, ni codigo, ni estado: el configurador no publica precios y este
 * dato no sale de la capa de aplicacion. Solo elige contra que lista preguntar
 * el techo incluido.
 */
public record PublishedPriceListRef(Long id, LocalDate validFrom, LocalDate validTo) {

    public PublishedPriceListRef {
        if (id == null) {
            throw new IllegalArgumentException("price list id is required");
        }
        if (validFrom == null) {
            throw new IllegalArgumentException("price list validFrom is required");
        }
    }
}
