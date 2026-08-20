package com.vetsoftware.app.registration.infrastructure.security;

import com.vetsoftware.app.registration.application.exception.CaptchaVerificationException;

/**
 * El captcha está activado pero mal configurado: falta el secreto, o el
 * proveedor rechaza la credencial con un 4xx.
 *
 * <p>
 * <b>Es la población que rompe el 100 % de los registros</b> (#99). Un token
 * caducado o un score bajo fallan para un usuario y son funcionamiento normal;
 * esto falla para todos, no lo puede arreglar nadie desde el navegador y sólo
 * se corrige tocando la configuración del despliegue. Mezclarlo con el fallo
 * corriente de captcha —como hacía el {@code catch (Exception)} único del
 * adaptador— dejaba la caída total escondida entre el ruido de los rechazos
 * legítimos.
 *
 * <p>
 * Extiende {@link CaptchaVerificationException} para que el flujo de registro
 * siga fallando cerrado si algún caller la trata genéricamente; el
 * {@code GlobalExceptionHandler} elige el handler más específico —igual que
 * hace con {@code BranchAccessDeniedException} frente a
 * {@code AccessDeniedException}— y es ahí, y sólo ahí, donde se registra (#99).
 */
public class CaptchaConfigurationException extends CaptchaVerificationException {

    public CaptchaConfigurationException(String message) {
        super(message);
    }

    public CaptchaConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
