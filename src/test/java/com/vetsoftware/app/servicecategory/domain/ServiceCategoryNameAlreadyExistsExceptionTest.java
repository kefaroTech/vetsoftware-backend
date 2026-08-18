package com.vetsoftware.app.servicecategory.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ServiceCategoryNameAlreadyExistsException")
class ServiceCategoryNameAlreadyExistsExceptionTest {

    @Test
    @DisplayName("el mensaje incluye el nombre repetido")
    void el_mensaje_incluye_el_nombre_repetido() {
        ServiceCategoryNameAlreadyExistsException ex = new ServiceCategoryNameAlreadyExistsException(
                "Consultas");

        assertThat(ex.getMessage()).contains("Consultas");
    }
}
