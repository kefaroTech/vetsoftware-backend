package com.vetsoftware.app.subscription.domain;

import java.time.LocalDate;

/**
 * Dos tramos del mismo articulo que se pisan (R7). El indice unico sobre
 * {@code current_item_marker} cubre el caso comun —dos lineas <em>abiertas</em>
 * del mismo articulo— pero <strong>no</strong> dos tramos con fechas de fin
 * futuras que se solapen: eso no es expresable en MySQL, que no tiene
 * restricciones de exclusion. Lo garantiza esta comprobacion.
 *
 * <p>
 * GlobalExceptionHandler: <strong>409</strong>,
 * {@code SUBSCRIPTION_ITEM_OVERLAP}.
 */
public class SubscriptionItemOverlapException extends RuntimeException {
    public SubscriptionItemOverlapException(Long catalogItemId, LocalDate from, LocalDate to) {
        super("Catalog item " + catalogItemId + " already has an effective line overlapping ["
                + from + ", " + (to == null ? "open" : to) + ")");
    }
}
