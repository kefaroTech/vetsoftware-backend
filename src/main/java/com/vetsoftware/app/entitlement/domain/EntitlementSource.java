package com.vetsoftware.app.entitlement.domain;

/**
 * De donde sale un permiso. Es lo que permite responder "por que esta clinica
 * ve facturacion" sin abrir el contrato.
 */
public enum EntitlementSource {
    /** Lo paga: hay linea de contrato vigente detras. */
    SUBSCRIPTION,
    /** Esta de prueba: caduca solo a la fecha, sin proceso que se olvide. */
    TRIAL,
    /** Viene con el nucleo del producto. */
    CORE,
    /** Se lo dieron a mano, y queda constancia de que fue a mano. */
    MANUAL_GRANT;

    /**
     * Espejo de {@code chk_company_entitlements_origin}: un permiso que dice venir
     * del contrato tiene que traer el contrato. Sin esto, un
     * {@code source = SUBSCRIPTION} con {@code subscription_id} nulo es un permiso
     * huerfano que el recalculo no sabe revocar.
     */
    public boolean requiresSubscription() {
        return this == SUBSCRIPTION || this == TRIAL;
    }
}
