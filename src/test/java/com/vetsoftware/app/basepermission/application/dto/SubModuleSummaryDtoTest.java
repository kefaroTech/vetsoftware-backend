package com.vetsoftware.app.basepermission.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.basepermission.domain.SubModuleRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SubModuleSummaryDto — mapeo desde SubModuleRef")
class SubModuleSummaryDtoTest {

    @Test
    @DisplayName("from() copia id, nombre y codigo de la referencia")
    void from_copia_id_nombre_y_codigo_de_la_referencia() {
        SubModuleSummaryDto dto = SubModuleSummaryDto.from(new SubModuleRef(1L, "Ventas", "VEN"));

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.name()).isEqualTo("Ventas");
        assertThat(dto.code()).isEqualTo("VEN");
    }
}
