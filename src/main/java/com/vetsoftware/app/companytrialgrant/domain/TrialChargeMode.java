package com.vetsoftware.app.companytrialgrant.domain;

/**
 * El modo de cobro que toma la línea del contrato. Es el <em>modo</em> de la
 * terna de {@link TrialPolicyOutcome}, no la política ni el desenlace.
 *
 * <p>
 * <strong>La columna que decide si se cobra es esta y solo esta.</strong> El
 * precio real sigue guardado dentro de la línea gratuita: una consulta de
 * facturación que filtre solo por vigencia y olvide el modo le cobra el precio
 * completo a todos los clientes en prueba. Y su gemela: el estado del contrato
 * deja de significar «a este cliente no se le cobra», porque un mismo contrato
 * lleva a la vez líneas en prueba y líneas de pago obligatorio.
 */
public enum TrialChargeMode {

    /** Gratis, con caducidad. */
    TRIAL,

    /** Se cobra. Es el único modo que devenga. */
    PAID,

    /** Gratis para siempre, pero con techo. */
    FREE_LIMITED,

    /**
     * La prueba venció y el artículo quedó en consulta. Dieciocho caracteres: la
     * columna necesita veinte, y con quince este desenlace sería literalmente
     * inescribible y el barrido nocturno fallaría sobre todos los clientes a la
     * vez.
     */
    EXPIRED_READ_ONLY;

    /** Solo un modo genera cargo. Los otros tres, jamás. */
    public boolean generatesCharge() {
        return this == PAID;
    }
}
