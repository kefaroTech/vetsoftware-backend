package com.vetsoftware.app.promotion.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.promotion.testsupport.PromotionMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CompanySummaryDto — proyeccion de CompanyRef")
class CompanySummaryDtoTest {

    @Test
    @DisplayName("from copia id, name e identifier del VO de dominio")
    void from_copia_los_tres_campos() {
        CompanySummaryDto dto = CompanySummaryDto.from(PromotionMother.CLINICA);

        assertThat(dto.id()).isEqualTo(PromotionMother.CLINICA.id());
        assertThat(dto.name()).isEqualTo(PromotionMother.CLINICA.name());
        assertThat(dto.identifier()).isEqualTo(PromotionMother.CLINICA.identifier());
    }
}
