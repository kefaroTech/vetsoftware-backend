package com.vetsoftware.app.openaccount.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("OwnerAlreadyHasOpenAccountException")
class OwnerAlreadyHasOpenAccountExceptionTest {

    @Test
    @DisplayName("el mensaje incluye el id del propietario")
    void el_mensaje_incluye_el_id_del_propietario() {
        OwnerAlreadyHasOpenAccountException ex = new OwnerAlreadyHasOpenAccountException(2L);

        assertThat(ex.getMessage()).contains("Owner already has an open account").contains("2");
    }
}
