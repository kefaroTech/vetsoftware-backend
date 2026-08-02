package com.vetsoftware.app.registration.application.port.out;

/**
 * Verifica el challenge de captcha del formulario de registro contra el
 * proveedor (reCAPTCHA). Cuando el captcha esta deshabilitado por
 * configuracion, la implementacion es un no-op.
 */
public interface CaptchaVerifier {
    void verify(String captchaToken, String remoteIp);
}
