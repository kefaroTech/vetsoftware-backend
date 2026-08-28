package com.vetsoftware.app.entitlement.domain;

import java.time.LocalDate;

/**
 * Companion VO del contrato desde el que se derivan los permisos. Trae lo justo
 * para decidir: que contrato es, en que estado esta, hasta cuando dura la
 * prueba y <strong>cuando se firmo</strong>.
 *
 * <p>
 * <strong>{@code signedOn} es {@code subscriptions.start_date}</strong>, y esa
 * eleccion tiene motivo. D-69 movio el <em>reloj de cobro</em> al vencimiento
 * de la prueba y dejo escrito que «la fecha de firma se conserva para todo lo
 * demas»: lo que se movio fue el ancla de facturacion
 * ({@code current_period_start}), no {@code start_date}, que sigue siendo el
 * dia en que el contrato empezo a existir. Es tambien la unica de las tres
 * candidatas que es {@code NOT NULL} y unica por contrato --
 * {@code subscription_items.effective_from} es por linea, y una renovacion
 * añade lineas nuevas con fecha de hoy, asi que usarla haria que renovar
 * <em>reactivara</em> limites que el cliente no acepto al firmar, que es
 * exactamente el daño que D-74 existe para impedir--.
 */
public record SubscriptionRef(Long id, ContractStatus status, LocalDate trialEndDate,
        LocalDate signedOn) {
    public SubscriptionRef {
        if (id == null)
            throw new IllegalArgumentException("subscription id is required");
        if (status == null)
            throw new IllegalArgumentException("subscription status is required");
        // NOT NULL en subscriptions.start_date. Nulo aqui dejaria a D-74 sin fecha
        // contra la que comparar y toda ausencia de fila volveria a leerse como
        // techo cero, en silencio.
        if (signedOn == null)
            throw new IllegalArgumentException("subscription signed on date is required");
        // Espejo de chk_subscriptions_trial: un contrato en prueba sin fecha de fin
        // de prueba es una prueba que no caduca nunca.
        if (status == ContractStatus.TRIALING && trialEndDate == null)
            throw new IllegalArgumentException("trial end date is required while TRIALING");
    }
}
