package com.vetsoftware.app.entitlement.domain;

/**
 * De donde sale un permiso. Es lo que permite responder "por que esta clinica
 * ve facturacion" sin abrir el contrato.
 *
 * <p>
 * <strong>Son seis, y los dos ultimos son el mecanismo central de la capa de
 * prueba</strong> (R-ENT-12). Con la lista corta de cuatro, la fila sucesora
 * --la que hace que al vencer la prueba el acceso <em>baje</em> en vez de
 * desaparecer-- era literalmente inescribible: no habia ningun valor que
 * significara "lo usa gratis con techo porque se le acabo la prueba", y esa es
 * justamente la pregunta de auditoria.
 */
public enum EntitlementSource {
    /** Lo paga: hay linea de contrato vigente detras. */
    SUBSCRIPTION,
    /** Esta de prueba: caduca solo a la fecha, sin proceso que se olvide. */
    TRIAL,
    /** Viene con el nucleo del producto. */
    CORE,
    /** Se lo dieron a mano, y queda constancia de que fue a mano. */
    MANUAL_GRANT,
    /**
     * Lo usa gratis, con techo, porque se le acabo la prueba. Sin este valor no se
     * distingue de "lo paga".
     */
    FREE_LIMITED,
    /**
     * La prueba vencio con desenlace de solo lectura (D-57): ve e imprime lo que
     * cargo, no crea nada nuevo. Es el unico camino por el que el modelo evita el
     * corte total de acceso que se prohibe a si mismo.
     */
    EXPIRED_TRIAL;

    /**
     * Espejo de {@code chk_company_entitlements_origin}: un permiso que dice venir
     * del contrato tiene que traer el contrato. Sin esto, un
     * {@code source = SUBSCRIPTION} con {@code subscription_id} nulo es un permiso
     * huerfano que el recalculo no sabe revocar.
     *
     * <p>
     * Los cuatro que lo exigen son exactamente los cuatro que nombra el
     * {@code CHECK}: los dos derivados del contrato y los dos que nacen de una
     * prueba que vencio, que tambien cuelgan de la linea que la concedio.
     */
    public boolean requiresSubscription() {
        return this == SUBSCRIPTION || this == TRIAL || this == FREE_LIMITED
                || this == EXPIRED_TRIAL;
    }
}
