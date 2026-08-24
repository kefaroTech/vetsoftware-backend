package com.vetsoftware.app.subscription.application.port.out;

import com.vetsoftware.app.subscription.application.dto.SubscriptionItemSnapshot;
import com.vetsoftware.app.subscription.domain.BillingCycle;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Resuelve catálogo y tramo de una tarifa publicada para una fecha y cantidad.
 */
public interface SubscriptionCommercialSnapshotPort {

    Optional<SubscriptionItemSnapshot> findPublishedItem(Long priceListId,
            BillingCycle billingCycle, Long catalogItemId, int quantity, LocalDate validOn);
}
