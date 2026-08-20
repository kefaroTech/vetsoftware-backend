package com.vetsoftware.app.registration.infrastructure.security;

import com.vetsoftware.app.registration.application.exception.CaptchaVerificationException;

/**
 * No se pudo hablar con el proveedor de captcha: timeout, corte de red o 5xx de
 * su lado.
 *
 * <p>
 * <b>Transitorio y ajeno</b> (#99). Se separa de
 * {@link CaptchaConfigurationException} porque exige lo contrario del operador:
 * aquí no hay nada que arreglar en este despliegue y la respuesta correcta es
 * esperar o reintentar, mientras que un secreto mal puesto no se arregla solo
 * nunca. Con las dos poblaciones en el mismo {@code catch} no había forma de
 * saber, leyendo el log, cuál de las dos cosas estaba pasando.
 *
 * <p>
 * Siempre lleva causa: es lo único que dice si fue un timeout de lectura o un
 * 503, y sin ella el punto único de registro no tendría nada que registrar.
 */
public class CaptchaProviderUnavailableException extends CaptchaVerificationException {

    public CaptchaProviderUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
