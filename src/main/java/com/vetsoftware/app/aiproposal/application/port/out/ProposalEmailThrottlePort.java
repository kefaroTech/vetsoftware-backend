package com.vetsoftware.app.aiproposal.application.port.out;

/**
 * El cupo de correos por destinatario.
 *
 * <p>
 * <strong>Un correo por propuesta y por hora, contado por
 * destinatario.</strong> Es un limite distinto del de
 * {@code /assistant/proposal}: aquel protege el gasto del modelo, este protege
 * que este producto no se convierta en un cañon de correo apuntable. Sin el,
 * quien quiera hostigar a una direccion solo tiene que pedir propuestas: cada
 * una manda un mensaje con SPF y DKIM validos hacia un buzon que nunca pidio
 * nada.
 *
 * <p>
 * <strong>La clave es un hash, no el correo.</strong> El contador vive en
 * Valkey; escribir la direccion en una clave de Redis es sacar el dato personal
 * de la tabla que lo protege y meterlo en un almacen que ni se anonimiza a los
 * 90 dias ni se purga a los 24 meses.
 */
public interface ProposalEmailThrottlePort {

    /**
     * @return {@code true} si queda cupo y el envio puede seguir; {@code false} si
     *         este destinatario ya consumio el suyo en la ventana actual
     */
    boolean tryAcquire(String contactEmail);
}
