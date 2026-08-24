package com.vetsoftware.app.quote.application.port.out;

import com.vetsoftware.app.quote.domain.PriceListRef;
import java.util.Optional;

/**
 * Resuelve la tarifa con la que se cotiza. Tabla global de plataforma, sin
 * tenant.
 */
public interface PriceListQueryPort {

    /**
     * Solo devuelve la lista si esta PUBLISHED. Cotizar contra un borrador
     * congelaria en un documento con valor legal unos precios que todavia se
     * estaban editando.
     */
    Optional<PriceListRef> findPublishedById(Long priceListId);
}
