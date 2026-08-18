package com.vetsoftware.app.service.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.service.testsupport.ServiceMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ServiceCategorySummaryDto — from(ServiceCategoryRef)")
class ServiceCategorySummaryDtoTest {

    @Test
    @DisplayName("copia los dos campos del ref")
    void copia_los_dos_campos_del_ref() {
        ServiceCategorySummaryDto dto = ServiceCategorySummaryDto.from(ServiceMother.CONSULTAS);

        assertThat(dto.id()).isEqualTo(ServiceMother.CONSULTAS.id());
        assertThat(dto.name()).isEqualTo(ServiceMother.CONSULTAS.name());
    }
}
