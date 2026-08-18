package com.vetsoftware.app.membership.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("MembershipHasActiveChildrenException — mensaje de dominio")
class MembershipHasActiveChildrenExceptionTest {

    @Test
    @DisplayName("el mensaje incluye el id de la membresia y el tipo de hijo que la bloquea")
    void el_mensaje_incluye_el_id_y_el_tipo_de_hijo() {
        MembershipHasActiveChildrenException ex = new MembershipHasActiveChildrenException(5L,
                "membershipSubModule");

        assertThat(ex.getMessage()).contains("5").contains("membershipSubModule");
    }
}
