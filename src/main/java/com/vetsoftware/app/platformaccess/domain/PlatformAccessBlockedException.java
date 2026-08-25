package com.vetsoftware.app.platformaccess.domain;

/**
 * Se agotaron los intentos del código de verificación. Sale como 429.
 *
 * <p>
 * El bloqueo es <b>terminal y permanente</b>, no «una hora»: agotados los cinco
 * intentos el token queda muerto para siempre y solo cabe pedir acceso de
 * nuevo. Un bloqueo temporal convertiría cinco intentos por hora en fuerza
 * bruta lenta sobre 10^6 combinaciones, y devolvería el estado a depender del
 * reloj.
 *
 * <p>
 * Gana a {@code EXPIRED} y a {@code PENDING} en la precedencia de estados: un
 * 429 tiene que seguir siendo 429 después de caducar el enlace, o el front
 * vuelve a ofrecer el formulario.
 */
public class PlatformAccessBlockedException extends RuntimeException {

    public PlatformAccessBlockedException(String message) {
        super(message);
    }
}
