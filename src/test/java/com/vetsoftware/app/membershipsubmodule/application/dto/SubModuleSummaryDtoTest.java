package com.vetsoftware.app.membershipsubmodule.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.membershipsubmodule.domain.SubModuleRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SubModuleSummaryDto.from — mapeo campo por campo")
class SubModuleSummaryDtoTest {

    @Test
    @DisplayName("copia cada campo del companion VO")
    void copia_cada_campo_del_companion_vo() {
        SubModuleRef ref = new SubModuleRef(980L, "Facturacion", "FACT");

        SubModuleSummaryDto dto = SubModuleSummaryDto.from(ref);

        assertThat(dto.id()).isEqualTo(980L);
        assertThat(dto.name()).isEqualTo("Facturacion");
        assertThat(dto.code()).isEqualTo("FACT");
    }
}
