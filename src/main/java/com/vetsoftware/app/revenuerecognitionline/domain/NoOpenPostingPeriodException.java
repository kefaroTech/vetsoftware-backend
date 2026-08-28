package com.vetsoftware.app.revenuerecognitionline.domain;

/**
 * No hay ningun periodo contable abierto en el que registrar el reconocimiento.
 *
 * <p>
 * <strong>Es el fallo que el diseño del esquema declara imposible y que hay que
 * saber ver igualmente.</strong> El disparador
 * {@code trg_accounting_periods_bu_guard} (§6.2 de la especificacion) impide
 * cerrar el ultimo periodo abierto justo para que un hecho tardio siempre tenga
 * donde escribirse. Si aun asi no queda ninguno —porque el disparador se cayo
 * en una migracion, o porque nadie abrio nunca el primero, que sigue siendo una
 * decision de negocio pendiente— lo correcto es <b>parar</b>: cualquier
 * alternativa (escribir en un mes cerrado, o inventarse uno) altera un periodo
 * ya declarado.
 */
public class NoOpenPostingPeriodException extends RuntimeException {

    public NoOpenPostingPeriodException(String periodKey) {
        super("No open accounting period at or after " + periodKey
                + ": open the next one before recognizing revenue");
    }
}
