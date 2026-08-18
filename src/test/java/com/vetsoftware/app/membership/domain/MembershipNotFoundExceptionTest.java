package com.vetsoftware.app.membership.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MembershipNotFoundException — mensaje de dominio")
class MembershipNotFoundExceptionTest {

    @Test
    @DisplayName("el mensaje incluye el id de la membresia no encontrada")
    void el_mensaje_incluye_el_id_de_la_membresia_no_encontrada() {
        MembershipNotFoundException ex = new MembershipNotFoundException(42L);

        assertThat(ex.getMessage()).contains("42").contains("Membership not found");
    }
}
