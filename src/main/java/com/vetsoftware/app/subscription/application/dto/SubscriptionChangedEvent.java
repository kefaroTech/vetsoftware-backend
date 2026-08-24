package com.vetsoftware.app.subscription.application.dto;

import com.vetsoftware.app.subscription.domain.SubscriptionChangeKind;
import java.time.LocalDate;

/**
 * El hecho de que el contrato cambio, para que quien corresponda recalcule.
 *
 * <p>
 * Este slice <strong>no publica ni recalcula entitlements</strong> —eso es del
 * slice {@code entitlement}—; lo que si hace es <em>exponer el hecho</em>, que
 * es lo que R11 necesita para poder dispararse: alta de contrato, alta o baja
 * de linea, cambio de cantidad, cambio de estado (incluidos {@code PAST_DUE} y
 * {@code READ_ONLY}) y cancelacion.
 */
public record SubscriptionChangedEvent(Long companyId, Long subscriptionId,
        SubscriptionChangeKind kind, LocalDate effectiveDate) {
}
