package com.vetsoftware.app.employeerole.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.employeerole.testsupport.EmployeeRoleMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("RoleSummaryDto")
class RoleSummaryDtoTest {

    @Test
    @DisplayName("from copia id, nombre y codigo desde el RoleRef")
    void from_copia_cada_campo() {
        RoleSummaryDto dto = RoleSummaryDto.from(EmployeeRoleMother.ROL_VETERINARIO);

        assertThat(dto.id()).isEqualTo(EmployeeRoleMother.ROL_VETERINARIO.id());
        assertThat(dto.name()).isEqualTo(EmployeeRoleMother.ROL_VETERINARIO.name());
        assertThat(dto.code()).isEqualTo(EmployeeRoleMother.ROL_VETERINARIO.code());
    }
}
