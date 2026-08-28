package com.vetsoftware.app.externalinvoicereconciliation.domain;

/**
 * Los cuatro desenlaces posibles de enfrentar lo que VetSoftware calculo contra
 * lo que el facturador externo emitio. Dominio cerrado y espejo exacto de
 * {@code chk_eir_status}: si aqui aparece un valor que la constraint no admite,
 * el {@code INSERT} lo rechaza la base y el fallo llega como un error sin
 * explicacion.
 *
 * <p>
 * <strong>{@link #MISSING_EXTERNAL} no es un valor mas de la lista: es el
 * estado inicial de la maquina.</strong> Un documento de cobro devengado que
 * nunca recibio factura externa es dinero que VetSoftware ya se apunto y que
 * <em>nadie facturo</em>. Es el peor de los cuatro y el mas facil de no ver,
 * <strong>porque no produce ninguna diferencia que llame la atencion</strong>:
 * los otros tres nacen de comparar dos numeros y saltan solos en cualquier
 * listado de descuadres; este no tiene con que compararse, asi que no aparece
 * en ninguno. Por eso la consulta que de verdad importa —y la que sirve
 * {@code ix_eir_pending (status, created_date)}— no es la de las diferencias
 * sino la bandeja de este estado.
 *
 * <p>
 * <strong>{@link #WITHIN_TOLERANCE} se llama tolerancia y no discrepancia por
 * un motivo aritmetico concreto</strong>, no por indulgencia: ver
 * {@link ExternalInvoiceReconciliation#TOLERANCIA}.
 */
public enum ExternalInvoiceReconciliationStatus {

    /** Los dos totales coinciden al centavo: {@code difference} vale cero. */
    MATCHED,

    /**
     * Hay diferencia, pero cabe en los dos pesos que separan calcular el impuesto
     * sobre la base agregada de calcularlo linea a linea.
     */
    WITHIN_TOLERANCE,

    /**
     * La diferencia se pasa de la tolerancia. Ya no la explica el redondeo del
     * impuesto: falta o sobra base, y alguien tiene que mirarlo.
     */
    MISMATCH,

    /**
     * Documento de cobro devengado que nunca recibio factura externa. Estado
     * inicial, y el unico en el que los cuatro campos de la pareja externa van
     * nulos ({@code chk_eir_external_pair}).
     */
    MISSING_EXTERNAL
}
