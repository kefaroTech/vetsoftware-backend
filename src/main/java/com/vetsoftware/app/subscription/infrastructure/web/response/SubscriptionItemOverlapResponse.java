package com.vetsoftware.app.subscription.infrastructure.web.response;

import java.time.LocalDate;

/**
 * Un solape detectado por la vigilancia R7. Cero elementos = sano; cualquier
 * elemento es un incidente de doble facturacion.
 */
public record SubscriptionItemOverlapResponse(Long companyId, Long subscriptionId,
        Long catalogItemId, String itemCode, Long firstItemId, LocalDate firstFrom,
        LocalDate firstTo, Long secondItemId, LocalDate secondFrom, LocalDate secondTo) {
}
