package com.vetsoftware.app.servicecategory.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ServiceCategoryNotFoundException")
class ServiceCategoryNotFoundExceptionTest {

    @Test
    @DisplayName("el mensaje incluye el id que no se encontro")
    void el_mensaje_incluye_el_id() {
        ServiceCategoryNotFoundException ex = new ServiceCategoryNotFoundException(70L);

        assertThat(ex.getMessage()).contains("ServiceCategory not found: 70");
    }
}
