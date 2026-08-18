package com.vetsoftware.app.employeerole.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.employeerole.testsupport.EmployeeRoleMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EmployeeSummaryDto")
class EmployeeSummaryDtoTest {

    @Test
    @DisplayName("from copia id, codigo y nombre desde el EmployeeRef")
    void from_copia_cada_campo() {
        EmployeeSummaryDto dto = EmployeeSummaryDto.from(EmployeeRoleMother.EMPLEADO);

        assertThat(dto.id()).isEqualTo(EmployeeRoleMother.EMPLEADO.id());
        assertThat(dto.employeeCode()).isEqualTo(EmployeeRoleMother.EMPLEADO.employeeCode());
        assertThat(dto.name()).isEqualTo(EmployeeRoleMother.EMPLEADO.name());
    }
}
