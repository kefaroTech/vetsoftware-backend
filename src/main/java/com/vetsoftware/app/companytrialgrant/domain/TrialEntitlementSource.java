package com.vetsoftware.app.companytrialgrant.domain;

/**
 * De dónde sale el permiso, visto desde la prueba. Companion de esta feature:
 * el dominio de {@code entitlement} no se importa.
 *
 * <p>
 * {@code FREE_LIMITED} es el valor que faltaba: sin él, «lo usa gratis con
 * techo porque se le acabó la prueba» no se distingue de «lo paga», y esa es
 * literalmente la pregunta de auditoría.
 */
public enum TrialEntitlementSource {

    /** Está en prueba. Caduca solo, por fecha. */
    TRIAL,

    /** Lo paga. */
    SUBSCRIPTION,

    /** Lo usa gratis, con techo, porque se le acabó la prueba. */
    FREE_LIMITED,

    /** La prueba venció con desenlace de solo lectura. */
    EXPIRED_TRIAL
}
