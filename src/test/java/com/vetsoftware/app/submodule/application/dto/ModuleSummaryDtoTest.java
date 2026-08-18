package com.vetsoftware.app.submodule.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.submodule.domain.ModuleRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ModuleSummaryDto — mapeo desde ModuleRef")
class ModuleSummaryDtoTest {

    @Test
    @DisplayName("from() copia id, nombre y codigo de la referencia")
    void from_copia_id_nombre_y_codigo_de_la_referencia() {
        ModuleSummaryDto dto = ModuleSummaryDto.from(new ModuleRef(1L, "Facturacion", "FACT"));

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.name()).isEqualTo("Facturacion");
        assertThat(dto.code()).isEqualTo("FACT");
    }
}
