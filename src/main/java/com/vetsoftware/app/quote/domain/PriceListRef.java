package com.vetsoftware.app.quote.domain;

import com.vetsoftware.app.shared.pricing.PriceListValidity;
import java.time.LocalDate;

/**
 * Companion VO de la tarifa con la que se cotizo, congelada en la cabecera.
 *
 * <p>
 * <b>Lleva su ventana de vigencia, y esa es la mitad del arreglo de D-73.</b>
 * Antes el VO solo traia id, codigo y moneda, asi que el unico filtro posible
 * era {@code status = 'PUBLISHED'} y <i>hoy se podia cotizar con la tarifa de
 * 2025</i>: bastaba con que una lista vieja siguiera publicada. La regla del
 * modelo dice que el precio sale de <b>la lista vigente por fecha</b>, nunca de
 * la primera que devuelva la consulta ni de un puntero de configuracion, y sin
 * las dos fechas aqui esa regla no se podia ni escribir.
 *
 * <p>
 * <b>La identidad de la tarifa vive aqui; la comparacion de fechas, en
 * {@link PriceListValidity}.</b> El kernel solo sabe de una ventana —dos fechas
 * y nada mas—, asi que es este VO, que si es de la rodaja, el que le pone id y
 * codigo al fallo. El camino del contrato aplica ese mismo predicado en su
 * cabecera con su propio companion VO, de modo que cotizar y firmar no pueden
 * discrepar sobre que significa «vigente»: la comparacion esta escrita una vez.
 */
public record PriceListRef(Long id, String code, String currency, LocalDate validFrom,
        LocalDate validTo) {

    public PriceListRef {
        if (id == null)
            throw new IllegalArgumentException("price list id is required");
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("price list code is required");
        if (currency == null || currency.isBlank())
            throw new IllegalArgumentException("price list currency is required");
        // La coherencia de las dos fechas la impone el kernel, que es quien manda
        // sobre la ventana. Se construye aqui para que un PriceListRef mal formado no
        // llegue a existir.
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
     * codigo en el fallo: el kernel no los conoce.
     */
    public void requireEffectiveOn(LocalDate date) {
        validity().requireEffectiveOn(date, id, code);
    }
}
