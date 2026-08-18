package com.vetsoftware.app.employeerole.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.employeerole.domain.EmployeeRole;
import com.vetsoftware.app.employeerole.testsupport.EmployeeRoleMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EmployeeRoleDto")
class EmployeeRoleDtoTest {

    @Test
    @DisplayName("from copia cada campo del agregado, incluidos los companion DTO")
    void from_copia_cada_campo() {
        EmployeeRole employeeRole = EmployeeRoleMother.habilitado();

        EmployeeRoleDto dto = EmployeeRoleDto.from(employeeRole);

        assertThat(dto.id()).isEqualTo(employeeRole.getId());
        assertThat(dto.employee()).isEqualTo(EmployeeSummaryDto.from(employeeRole.getEmployee()));
        assertThat(dto.role()).isEqualTo(RoleSummaryDto.from(employeeRole.getRole()));
        assertThat(dto.createdDate()).isEqualTo(employeeRole.getCreatedDate());
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("from conserva el estado deshabilitado")
    void from_conserva_el_estado_deshabilitado() {
        EmployeeRoleDto dto = EmployeeRoleDto.from(EmployeeRoleMother.deshabilitado());

        assertThat(dto.enabled()).isFalse();
    }
}
