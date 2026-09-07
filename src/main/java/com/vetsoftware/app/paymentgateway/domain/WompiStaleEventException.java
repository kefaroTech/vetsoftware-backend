package com.vetsoftware.app.paymentgateway.domain;

/**
 * El {@code timestamp} del webhook de Wompi está fuera de la ventana de
 * frescura configurada
 * ({@code vetsoftware.payments.wompi.event-freshness-tolerance}). Se mapea a
 * 401, igual que {@link WompiChecksumMismatchException}: un evento antiguo,
 * aunque esté firmado correctamente, no se acepta como aviso vigente.
 */
public class WompiStaleEventException extends RuntimeException {

    public WompiStaleEventException(String message) {
        super(message);
    }
}
