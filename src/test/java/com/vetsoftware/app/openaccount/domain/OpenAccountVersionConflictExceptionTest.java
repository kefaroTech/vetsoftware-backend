package com.vetsoftware.app.openaccount.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("OpenAccountVersionConflictException")
class OpenAccountVersionConflictExceptionTest {

    @Test
    @DisplayName("el mensaje incluye el id de la cuenta y las dos versiones")
    void el_mensaje_incluye_id_y_versiones() {
        OpenAccountVersionConflictException ex = new OpenAccountVersionConflictException(77L, 3L,
                5L);

        assertThat(ex.getMessage()).contains("77").contains("expected 3").contains("current is 5");
    }
}
