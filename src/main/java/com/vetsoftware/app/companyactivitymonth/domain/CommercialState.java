package com.vetsoftware.app.companyactivitymonth.domain;

/**
 * En que relacion comercial estaba la clinica <b>ese mes</b>.
 *
 * <p>
 * Espejo de {@code chk_cam_state}. Los cuatro codigos son decision del
 * changeset 355: la ficha del modelo decia «pagando, gratuito, en prueba o ido»
 * y nunca dio los literales, asi que se fijaron ahi y aqui se copian tal cual.
 * Un quinto valor en cualquiera de los dos lados es una fila que el motor
 * rechaza o un estado que Java no sabe leer.
 *
 * <p>
 * <strong>El estado es del mes, no de la empresa.</strong> Guardarlo por mes es
 * lo que permite reconstruir la serie —cuando entro en prueba, cuando empezo a
 * pagar, cuando se fue— sin recalcular el pasado. Una columna «estado actual»
 * sobre {@code companies} contestaria solo por hoy.
 */
public enum CommercialState {
    /** Mes facturado: la clinica estaba pagando. */
    PAID,
    /** Mes servido sin cobro, fuera de una ventana de prueba. */
    FREE,
    /** Mes dentro de la ventana de prueba gratuita. */
    TRIAL,
    /** Mes posterior a la baja. La serie sigue: irse tambien es un dato. */
    CHURNED
}
