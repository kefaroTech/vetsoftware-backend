package com.vetsoftware.app.laboratorytestfile.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.laboratorytestfile.domain.EmployeeRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EmployeeSummaryDto — from()")
class EmployeeSummaryDtoTest {

    @Test
    @DisplayName("copia cada campo del ref")
    void copia_cada_campo_del_ref() {
        EmployeeRef ref = new EmployeeRef(4L, "EMP-001", "Ana Ruiz");

        EmployeeSummaryDto dto = EmployeeSummaryDto.from(ref);

        assertThat(dto.id()).isEqualTo(4L);
        assertThat(dto.employeeCode()).isEqualTo("EMP-001");
        assertThat(dto.name()).isEqualTo("Ana Ruiz");
    }
}
