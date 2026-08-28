package com.vetsoftware.app.companycontactchannel.domain;

/**
 * Para que se autorizo el canal. Espejo <strong>literal</strong> de
 * {@code chk_company_contact_channels_purpose}.
 *
 * <p>
 * <strong>Autorizar un proposito no autoriza los demas, y por eso esto es una
 * columna y no un comentario.</strong> Cuando la finalidad es comercial mandan
 * las dos cosas a la vez —canal autorizado Y finalidad autorizada—, y
 * mezclarlas es la forma mas rapida de convertir un aviso util en una queja: el
 * correo que el cliente dio para recibir su factura no es permiso para mandarle
 * una promocion.
 *
 * <p>
 * <strong>Tambien es la mitad de la clave del canal primario.</strong> El
 * indice unico del esquema es {@code (primary_marker, purpose)}, asi que hay un
 * primario por empresa Y proposito: el correo de facturacion y el movil de mora
 * conviven, cada uno primario de lo suyo.
 */
public enum ContactPurpose {

    /** Facturas y cobros ordinarios. */
    BILLING,

    /** Gestion de mora. */
    DUNNING,

    /** Avisos del servicio. */
    OPERATIONAL,

    /** Comunicacion comercial. */
    MARKETING
}
