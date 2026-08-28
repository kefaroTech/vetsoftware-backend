package com.vetsoftware.app.subscription.application.port.out;

import com.vetsoftware.app.subscription.application.dto.PublishedCatalogItem;
import com.vetsoftware.app.subscription.domain.BillingCycle;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Resuelve el articulo activo y TODOS los tramos de la tarifa publicada vigente
 * en una fecha.
 *
 * <p>
 * Es la unica fuente de precio de este slice: ni el importe, ni el nombre, ni
 * la unidad, ni lo incluido llegan nunca del cuerpo de una peticion. El
 * {@code quantity} sirve para comprobar los minimos y maximos del articulo, no
 * para elegir tramo: los tramos vienen todos y el reparto lo hace el dominio
 * (D-66).
 */
public interface SubscriptionCommercialSnapshotPort {

    Optional<PublishedCatalogItem> findPublishedItem(Long priceListId, BillingCycle billingCycle,
            Long catalogItemId, int quantity, LocalDate validOn);
}
