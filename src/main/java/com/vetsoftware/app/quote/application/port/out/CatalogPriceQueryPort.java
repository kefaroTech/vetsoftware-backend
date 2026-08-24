package com.vetsoftware.app.quote.application.port.out;

import com.vetsoftware.app.quote.domain.BillingCycle;
import com.vetsoftware.app.quote.domain.CatalogPriceRef;
import java.util.Optional;

/**
 * Lee el precio vigente del articulo en la tarifa cotizada, UNA SOLA VEZ, para
 * congelarlo en la linea.
 *
 * <p>
 * El precio nunca llega del cliente: si el importe fuera un campo del
 * formulario, cotizar a cero seria trivial.
 */
public interface CatalogPriceQueryPort {

    /**
     * Tramo aplicable a esa cantidad: tier_min &lt;= quantity y (tier_max nulo o
     * &gt;= quantity), el de tier_min mas alto.
     */
    Optional<CatalogPriceRef> findApplicable(Long priceListId, Long catalogItemId,
            BillingCycle billingCycle, int quantity);
}
