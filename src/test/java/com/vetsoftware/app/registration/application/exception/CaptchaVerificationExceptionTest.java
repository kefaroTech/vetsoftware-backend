package com.vetsoftware.app.registration.application.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CaptchaVerificationException")
class CaptchaVerificationExceptionTest {

    @Test
    @DisplayName("expone el mensaje recibido")
    void expone_el_mensaje_recibido() {
        CaptchaVerificationException exception = new CaptchaVerificationException(
                "Captcha validation failed");

        assertThat(exception.getMessage()).isEqualTo("Captcha validation failed");
    }
}
