package com.vetsoftware.app.subscriptionpaymentmethod.domain;

/**
 * Con que se le cobra al cliente. Espejo exacto de
 * {@code chk_subscription_payment_methods_kind}, y <strong>cerrado a dos
 * valores a proposito</strong>.
 *
 * <p>
 * La ficha original enumeraba tambien transferencia y efectivo, que D-48
 * <strong>prohibe expresamente</strong> como medio de cobro de la suscripcion.
 * Un valor que la propia decision prohibe no es una opcion de mas: es una
 * puerta abierta esperando a que alguien la use, y el dia que se use la base lo
 * rechaza con un 409 sin explicacion. Aqui si se pudo cerrar la lista porque la
 * tabla nace con el changeset 319, a diferencia de
 * {@code chk_subscription_payments_method}, donde estrecharla habria roto los
 * enums de un dominio ya en produccion.
 *
 * <p>
 * <strong>No se llama {@code PaymentMethod}</strong>: ese nombre ya lo ocupa el
 * enum de {@code subscriptionpayment.domain}, y springdoc funde los esquemas
 * del contrato por nombre simple. Dos enums distintos con el mismo nombre
 * simple se fusionarian en el {@code api/openapi.json} y los dos frontends
 * generarian un tipo que no es ninguno de los dos.
 */
public enum PaymentMethodKind {

    /** Tarjeta: caduca, y por eso es la unica que exige {@code expiresOn}. */
    CARD,

    /** Debito automatico PSE: no caduca, asi que no lleva fecha de expiracion. */
    PSE
}
