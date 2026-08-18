package com.vetsoftware.app.baserolepermission.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.baserolepermission.domain.BaseRoleRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BaseRoleSummaryDto — mapeo desde BaseRoleRef")
class BaseRoleSummaryDtoTest {

    @Test
    @DisplayName("from() copia id, nombre y codigo de la referencia")
    void from_copia_id_nombre_y_codigo_de_la_referencia() {
        BaseRoleSummaryDto dto = BaseRoleSummaryDto.from(new BaseRoleRef(1L, "Veterinario", "VET"));

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.name()).isEqualTo("Veterinario");
        assertThat(dto.code()).isEqualTo("VET");
    }
}
