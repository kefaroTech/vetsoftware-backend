package com.vetsoftware.app.subscriptionbilling.domain;

/**
 * Dónde está un cargo en el circuito devengar → facturar.
 *
 * <p>
 * Espejo de {@code chk_subscription_charges_status}. Junto con
 * {@code billing_document_id} es el <b>único</b> campo mutable de un cargo: los
 * importes no cambian nunca (R1 de {@code suscripciones-reglas-codigo.md}), y
 * por eso {@code SubscriptionChargeJpaEntity} va exenta de {@code @Version} con
 * el código {@code E6_YA_PROTEGIDO}.
 */
public enum ChargeStatus {
    /** Devengado y todavía sin factura. Es lo que recoge el proceso mensual. */
    PENDING,
    /**
     * Ya salió en un documento de cobro. {@code chk_subscription_charges_invoiced}
     * exige que entonces {@code billing_document_id} no sea nulo.
     */
    INVOICED,
    /** Compensado por un cargo negativo. Los dos quedan y suman cero. */
    VOIDED
}
