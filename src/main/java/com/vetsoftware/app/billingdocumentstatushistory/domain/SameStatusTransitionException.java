package com.vetsoftware.app.billingdocumentstatushistory.domain;

/**
 * Alguien intento apuntar que un documento paso de un estado <strong>a ese
 * mismo estado</strong>.
 *
 * <p>
 * Espejo de {@code chk_bdsh_transition} ({@code from_status <> to_status}), y
 * la excepcion existe para que el motor no sea el primero en enterarse: un
 * choque de {@code CHECK} llega al operador como un 500 sin explicacion,
 * mientras que esto se puede leer.
 *
 * <p>
 * <strong>Por que es un rechazo y no un no-op silencioso.</strong> Una fila que
 * dice que algo paso de {@code DRAFT} a {@code DRAFT} no es historia, es ruido,
 * y ensucia exactamente la consulta para la que esta tabla existe: contar
 * cuantos documentos estaban en un estado a una fecha se hace mirando la ultima
 * transicion anterior a esa fecha, y una transicion vacia desplaza la buena sin
 * cambiar nada. Tragarsela en silencio seria peor todavia, porque el llamador
 * creeria haber registrado un cambio que no ocurrio.
 */
public class SameStatusTransitionException extends RuntimeException {

    public SameStatusTransitionException(Long billingDocumentId, BillingDocumentStatus status) {
        super("Billing document " + billingDocumentId + " did not change status: it stayed in "
                + status);
    }
}
