package com.vetsoftware.app.platformaccess.domain;

/**
 * El código de 6 dígitos no coincide con el de la solicitud. Sale como 422.
 *
 * <p>
 * {@code remainingAttempts} es opcional en el contrato: el front omite la
 * cuenta atrás si no viene, en vez de inventar un número. Aquí siempre se
 * conoce —{@code max_attempts - verification_attempts} releído tras el
 * {@code UPDATE} atómico— así que siempre viaja.
 */
public class PlatformAccessCodeMismatchException extends RuntimeException {

    private final int remainingAttempts;

    public PlatformAccessCodeMismatchException(String message, int remainingAttempts) {
        super(message);
        this.remainingAttempts = remainingAttempts;
    }

    public int getRemainingAttempts() {
        return remainingAttempts;
    }
}
