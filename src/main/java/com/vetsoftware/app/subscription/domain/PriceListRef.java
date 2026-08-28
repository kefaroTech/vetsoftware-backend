package com.vetsoftware.app.subscription.domain;

import com.vetsoftware.app.shared.pricing.PriceListValidity;
import java.time.LocalDate;

/**
 * Companion VO de la tarifa contra la que se firma la cabecera del contrato.
 *
 * <p>
 * <b>Es la rodaja de contrato la que nombra la tarifa</b>, no el kernel.
 * {@link PriceListValidity} solo sabe de dos fechas —ese es su criterio de
 * admision: lo compartido no referencia ninguna feature—, asi que el id y el
 * codigo viven aqui, que es el sitio que si puede saber que existen las
 * tarifas. {@code price_lists} es global de plataforma y no lleva
 * {@code company_id}, por eso este VO tampoco.
 *
 * <p>
 * Es el gemelo de {@code quote.domain.PriceListRef} y no comparte tipo con el a
 * proposito: son dos rodajas y cada una define el suyo. Lo que si comparten —y
 * es lo unico que tenian que compartir— es el predicado de vigencia.
 */
public record PriceListRef(Long id, String code, LocalDate validFrom, LocalDate validTo) {

    public PriceListRef {
        if (id == null)
            throw new IllegalArgumentException("price list id is required");
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("price list code is required");
        new PriceListValidity(validFrom, validTo);
    }

    /** La ventana suelta, para quien solo necesita decidir vigencia. */
    public PriceListValidity validity() {
        return new PriceListValidity(validFrom, validTo);
    }

    /** @see PriceListValidity#isEffectiveOn(LocalDate) */
    public boolean isEffectiveOn(LocalDate date) {
        return validity().isEffectiveOn(date);
    }

    /**
     * Exige que la tarifa este vigente ese dia, y si no lo esta pone su id y su
     * codigo en el fallo, para que el 409 diga QUE tarifa y con que ventana.
     */
    public void requireEffectiveOn(LocalDate date) {
        validity().requireEffectiveOn(date, id, code);
    }
}
