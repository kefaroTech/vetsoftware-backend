package com.vetsoftware.app.hospitalizationprocedure.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.hospitalizationprocedure.domain.EmployeeRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EmployeeSummaryDto.from")
class EmployeeSummaryDtoTest {

    @Test
    @DisplayName("copia los tres campos del ref")
    void copia_los_tres_campos_del_ref() {
        EmployeeRef ref = new EmployeeRef(4L, "EMP-001", "Ana Ruiz");

        EmployeeSummaryDto dto = EmployeeSummaryDto.from(ref);

        assertThat(dto.id()).isEqualTo(4L);
        assertThat(dto.employeeCode()).isEqualTo("EMP-001");
        assertThat(dto.name()).isEqualTo("Ana Ruiz");
    }
}
