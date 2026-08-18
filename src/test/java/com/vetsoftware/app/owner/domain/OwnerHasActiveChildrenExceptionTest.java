package com.vetsoftware.app.owner.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("OwnerHasActiveChildrenException")
class OwnerHasActiveChildrenExceptionTest {

    @Test
    @DisplayName("compone el mensaje con el id del owner y el tipo de hijo")
    void compone_el_mensaje_con_el_id_y_el_tipo_de_hijo() {
        OwnerHasActiveChildrenException exception = new OwnerHasActiveChildrenException(10L,
                "animal");

        assertThat(exception.getMessage())
                .isEqualTo("Cannot delete owner 10: has active animal children");
    }
}
