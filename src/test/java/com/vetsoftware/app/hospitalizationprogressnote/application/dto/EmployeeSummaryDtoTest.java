package com.vetsoftware.app.hospitalizationprogressnote.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.hospitalizationprogressnote.domain.EmployeeRef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("EmployeeSummaryDto")
class EmployeeSummaryDtoTest {

    @Test
    @DisplayName("from() copia id, codigo y nombre del companion VO")
    void from_copia_cada_campo() {
        EmployeeRef ref = new EmployeeRef(4L, "EMP-001", "Ana Ruiz");

        EmployeeSummaryDto dto = EmployeeSummaryDto.from(ref);

        assertThat(dto.id()).isEqualTo(4L);
        assertThat(dto.employeeCode()).isEqualTo("EMP-001");
        assertThat(dto.name()).isEqualTo("Ana Ruiz");
    }
}
