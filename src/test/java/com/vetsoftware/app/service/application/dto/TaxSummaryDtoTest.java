package com.vetsoftware.app.service.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.service.testsupport.ServiceMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("TaxSummaryDto — from(TaxRef)")
class TaxSummaryDtoTest {

    @Test
    @DisplayName("copia los tres campos del ref")
    void copia_los_tres_campos_del_ref() {
        TaxSummaryDto dto = TaxSummaryDto.from(ServiceMother.IVA_19);

        assertThat(dto.id()).isEqualTo(ServiceMother.IVA_19.id());
        assertThat(dto.name()).isEqualTo(ServiceMother.IVA_19.name());
        assertThat(dto.percentage()).isEqualByComparingTo(ServiceMother.IVA_19.percentage());
    }
}
