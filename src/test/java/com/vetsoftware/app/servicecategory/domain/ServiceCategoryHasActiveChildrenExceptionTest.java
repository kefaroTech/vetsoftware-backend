package com.vetsoftware.app.servicecategory.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ServiceCategoryHasActiveChildrenException")
class ServiceCategoryHasActiveChildrenExceptionTest {

    @Test
    @DisplayName("el mensaje incluye el id de la categoria y el tipo de hijo")
    void el_mensaje_incluye_id_y_tipo_de_hijo() {
        ServiceCategoryHasActiveChildrenException ex = new ServiceCategoryHasActiveChildrenException(
                70L, "service");

        assertThat(ex.getMessage()).contains("70").contains("service");
    }
}
