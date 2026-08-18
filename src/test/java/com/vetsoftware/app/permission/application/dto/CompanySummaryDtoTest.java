package com.vetsoftware.app.permission.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.permission.testsupport.PermissionMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CompanySummaryDto")
class CompanySummaryDtoTest {

    @Test
    @DisplayName("from() copia id, nombre e identificador del ref")
    void from_copia_los_campos_del_ref() {
        CompanySummaryDto dto = CompanySummaryDto.from(PermissionMother.CLINICA);

        assertThat(dto.id()).isEqualTo(PermissionMother.CLINICA.id());
        assertThat(dto.name()).isEqualTo(PermissionMother.CLINICA.name());
        assertThat(dto.identifier()).isEqualTo(PermissionMother.CLINICA.identifier());
    }
}
