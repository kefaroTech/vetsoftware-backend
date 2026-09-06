package com.vetsoftware.app.paymentgateway.domain;

/**
 * El {@code X-Event-Checksum} del webhook no coincide con el calculado sobre el
 * cuerpo recibido. Se mapea a <strong>401, no 200</strong>: acusar recibo de un
 * evento que no se pudo autenticar dejaría a cualquiera con la URL pública
 * mover el estado de un pago a su antojo.
 */
public class WompiChecksumMismatchException extends RuntimeException {

    public WompiChecksumMismatchException(String message) {
        super(message);
    }
}
