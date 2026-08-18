package com.vetsoftware.app.baserolepermission.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.baserolepermission.domain.BasePermissionRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BasePermissionSummaryDto — mapeo desde BasePermissionRef")
class BasePermissionSummaryDtoTest {

    @Test
    @DisplayName("from() copia id, nombre y codigo de la referencia")
    void from_copia_id_nombre_y_codigo_de_la_referencia() {
        BasePermissionSummaryDto dto = BasePermissionSummaryDto
                .from(new BasePermissionRef(10L, "Crear consulta", "CONSULTA_CREATE"));

        assertThat(dto.id()).isEqualTo(10L);
        assertThat(dto.name()).isEqualTo("Crear consulta");
        assertThat(dto.code()).isEqualTo("CONSULTA_CREATE");
    }
}
