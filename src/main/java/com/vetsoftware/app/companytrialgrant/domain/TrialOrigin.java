package com.vetsoftware.app.companytrialgrant.domain;

/**
 * Quién concedió la prueba. {@code chk_company_trial_grants_paper}.
 *
 * <p>
 * Tres papeles posibles, y el alta pública es uno de ellos, no la ausencia de
 * los otros dos: la propia política del catálogo concede la prueba de todo
 * artículo {@code ELIGIBLE} al registrarse, sin cotización ni otrosí detrás.
 */
public enum TrialOrigin {

    /** La política del catálogo la concedió sola, al registrarse. */
    SIGNUP,

    /** Nació de una cotización aceptada. */
    QUOTE,

    /** Nació de un otrosí sobre un contrato ya vivo. */
    AMENDMENT
}
