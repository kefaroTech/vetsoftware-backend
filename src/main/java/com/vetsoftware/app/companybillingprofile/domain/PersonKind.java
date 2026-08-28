package com.vetsoftware.app.companybillingprofile.domain;

/**
 * Que clase de tercero es el destinatario de la factura. Dominio cerrado y
 * espejo <strong>literal</strong> de
 * {@code chk_company_billing_profiles_person_kind}: los dos nombres se escriben
 * aqui igual que en la constraint porque {@code @Enumerated(EnumType.STRING)}
 * guarda el {@code name()} tal cual, y un valor que la comprobacion no admita
 * lo rechaza la base con un error que no menciona ni la columna ni el valor.
 *
 * <p>
 * <strong>No es un detalle de presentacion: decide que columnas de nombre son
 * obligatorias y cuales tienen que ir vacias.</strong> Esa es la otra
 * constraint, {@code chk_company_billing_profiles_name_shape}, y su espejo vive
 * en {@link CompanyBillingProfile}.
 */
public enum PersonKind {

    /**
     * Persona natural. La informacion exogena anual exige primer nombre, otros
     * nombres, primer apellido y segundo apellido <em>por separado</em>, asi que
     * aqui van los cuatro campos partidos y {@code legal_name} vacio.
     */
    NATURAL,

    /**
     * Sociedad. Un solo {@code legal_name} y los cuatro campos de persona natural
     * vacios.
     */
    LEGAL
}
