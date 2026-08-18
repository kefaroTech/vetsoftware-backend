package com.vetsoftware.app.service.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.service.testsupport.ServiceMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CompanySummaryDto — from(CompanyRef)")
class CompanySummaryDtoTest {

    @Test
    @DisplayName("copia los tres campos del ref")
    void copia_los_tres_campos_del_ref() {
        CompanySummaryDto dto = CompanySummaryDto.from(ServiceMother.CLINICA);

        assertThat(dto.id()).isEqualTo(ServiceMother.CLINICA.id());
        assertThat(dto.name()).isEqualTo(ServiceMother.CLINICA.name());
        assertThat(dto.identifier()).isEqualTo(ServiceMother.CLINICA.identifier());
    }
}
