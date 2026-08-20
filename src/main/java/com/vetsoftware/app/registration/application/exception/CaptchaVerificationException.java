package com.vetsoftware.app.registration.application.exception;

/**
 * El challenge de captcha del registro no se pudo dar por bueno. Se mapea a 400
 * {@code CAPTCHA_FAILED}.
 *
 * <p>
 * Esta clase representa el caso <b>atribuible a quien envía el formulario</b>:
 * token ausente, caducado, ya usado o con score por debajo del mínimo. Las dos
 * causas que no son del usuario tienen subclase propia en el adaptador
 * ({@code CaptchaConfigurationException} y
 * {@code CaptchaProviderUnavailableException}), porque piden acciones distintas
 * del operador (#99).
 */
public class CaptchaVerificationException extends RuntimeException {

    public CaptchaVerificationException(String message) {
        super(message);
    }

    /**
     * Con causa. Sin este constructor el fallo de la llamada al proveedor llegaba
     * al handler pelado y el punto único de registro no tenía nada que registrar
     * (#99): el «qué pasó» —timeout de lectura, 503, credencial rechazada— vive
     * entero en la causa.
     */
    public CaptchaVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
