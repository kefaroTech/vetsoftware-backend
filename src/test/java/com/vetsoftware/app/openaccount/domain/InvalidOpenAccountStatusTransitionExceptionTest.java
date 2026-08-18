package com.vetsoftware.app.openaccount.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("InvalidOpenAccountStatusTransitionException")
class InvalidOpenAccountStatusTransitionExceptionTest {

    @Test
    @DisplayName("el mensaje nombra el estado de origen y el de destino")
    void el_mensaje_nombra_origen_y_destino() {
        InvalidOpenAccountStatusTransitionException ex = new InvalidOpenAccountStatusTransitionException(
                OpenAccountStatus.CLOSE, OpenAccountStatus.OPEN);

        assertThat(ex.getMessage())
                .contains("cannot change open account status from CLOSE to OPEN");
    }
}
