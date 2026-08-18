package com.vetsoftware.app.permission.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.permission.testsupport.PermissionMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SubModuleSummaryDto")
class SubModuleSummaryDtoTest {

    @Test
    @DisplayName("from() copia id, nombre y code del ref")
    void from_copia_los_campos_del_ref() {
        SubModuleSummaryDto dto = SubModuleSummaryDto.from(PermissionMother.INVENTARIO);

        assertThat(dto.id()).isEqualTo(PermissionMother.INVENTARIO.id());
        assertThat(dto.name()).isEqualTo(PermissionMother.INVENTARIO.name());
        assertThat(dto.code()).isEqualTo(PermissionMother.INVENTARIO.code());
    }
}
