package com.vetsoftware.app.subscription.application.port.out;

/**
 * Asigna los dos consecutivos citables de este slice: {@code SUS-AAAA-NNNNN}
 * para el contrato y {@code AMD-AAAA-NNNNN} para el otrosi.
 *
 * <p>
 * Los dos son <b>numeros que se citan</b> —el del contrato en soporte y en
 * cobranza, el del otrosi como el papel del cambio—, y un numero citable que
 * escribe quien quiera no es citable. Por eso no viajan en el cuerpo de la
 * peticion: los pone el servidor.
 *
 * <p>
 * <b>Ninguna implementacion puede sacarlos de un «maximo mas uno».</b> Esa
 * forma serializa a los concurrentes solo <i>si la fila existe</i>, y el primer
 * numero de la serie no tiene fila que bloquear: ahi el candado no bloquea
 * nada, dos altas simultaneas calculan el mismo numero y la segunda revienta
 * contra el indice unico. Es el defecto contra el que advierte la
 * especificacion al justificar por que el contador es una tabla y no una
 * consulta.
 *
 * <p>
 * <b>La reserva vive dentro de la transaccion de negocio</b> que la pide: si el
 * alta falla, el numero vuelve a estar libre y la serie no queda con un hueco.
 * Es la diferencia deliberada con el consecutivo fiscal de la DIAN, que si debe
 * conservarlo.
 */
public interface SubscriptionNumberPort {

    /** El siguiente {@code SUS-AAAA-NNNNN} del ano, reservado de forma atomica. */
    String nextSubscriptionNumber(int year);

    /** El siguiente {@code AMD-AAAA-NNNNN} del ano, reservado de forma atomica. */
    String nextAmendmentNumber(int year);
}
