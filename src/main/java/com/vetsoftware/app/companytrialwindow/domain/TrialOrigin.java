package com.vetsoftware.app.companytrialwindow.domain;

/**
 * Quién abrió la ventana. {@code chk_company_trial_windows_origin}.
 *
 * <p>
 * Solo hay dos vías de abrir el reloj de una empresa: el alta pública, que no
 * negocia nada, y una cotización aceptada. No hay una tercera porque una
 * empresa nace de una de esas dos formas y no de ninguna otra.
 */
public enum TrialOrigin {

    /** La política del catálogo la abrió sola, al registrarse sin cotización. */
    SIGNUP,

    /** Nació al aceptar una cotización. */
    QUOTE
}
