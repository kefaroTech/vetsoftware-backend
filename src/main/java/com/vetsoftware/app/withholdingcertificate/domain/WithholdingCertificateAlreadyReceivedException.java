package com.vetsoftware.app.withholdingcertificate.domain;

import java.time.LocalDate;

/**
 * El certificado ya llego, y por eso las dos segundas escrituras estan
 * cerradas.
 *
 * <p>
 * <strong>Es la unica regla de este agregado que la base NO cuida.</strong> El
 * {@code CHECK} del esquema impide que un sustituto conviva con un
 * {@code received_on}, asi que adjuntar el comprobante de pago sobre un
 * certificado recibido lo para el motor; lo que el motor deja pasar
 * tranquilamente es <em>volver a recibirlo</em>, porque un {@code UPDATE} que
 * pisa {@code received_on} y {@code file_ref} con otros valores es una fila
 * perfectamente valida. Y pisar esos dos campos borra el archivo que ya se
 * habia guardado: el expediente pierde el soporte y nadie se entera hasta que
 * hay que probar la retencion.
 *
 * <p>
 * Es 409 y no 400: el cuerpo esta bien escrito y lo que choca es el estado del
 * certificado en este instante. Un 400 le diria al operador que corrija un
 * campo que esta correcto.
 */
public class WithholdingCertificateAlreadyReceivedException extends RuntimeException {

    public WithholdingCertificateAlreadyReceivedException(Long id, LocalDate receivedOn) {
        super("Withholding certificate " + id + " was already received on " + receivedOn);
    }
}
