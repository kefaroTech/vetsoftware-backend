package com.vetsoftware.app.laboratorytesttype.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.laboratorytesttype.domain.LaboratoryTestType;
import com.vetsoftware.app.laboratorytesttype.testsupport.LaboratoryTestTypeMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("LaboratoryTestTypeDto.from")
class LaboratoryTestTypeDtoTest {

    @Test
    @DisplayName("copia cada campo del tipo propio de una empresa, incluida su company")
    void copia_cada_campo_del_tipo_propio_de_empresa() {
        LaboratoryTestType tipo = LaboratoryTestTypeMother.propioDeEmpresa();

        LaboratoryTestTypeDto dto = LaboratoryTestTypeDto.from(tipo);

        assertThat(dto.id()).isEqualTo(tipo.getId());
        assertThat(dto.name()).isEqualTo(tipo.getName());
        assertThat(dto.description()).isEqualTo(tipo.getDescription());
        assertThat(dto.company()).isEqualTo(CompanySummaryDto.from(tipo.getCompany()));
        assertThat(dto.general()).isFalse();
        assertThat(dto.createdDate()).isEqualTo(tipo.getCreatedDate());
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("un tipo general proyecta company nula sin invocar CompanySummaryDto.from")
    void un_tipo_general_proyecta_company_nula() {
        LaboratoryTestType general = LaboratoryTestTypeMother.general();

        LaboratoryTestTypeDto dto = LaboratoryTestTypeDto.from(general);

        assertThat(dto.company()).isNull();
        assertThat(dto.general()).isTrue();
    }

    @Test
    @DisplayName("propaga el estado deshabilitado")
    void propaga_el_estado_deshabilitado() {
        LaboratoryTestType tipo = LaboratoryTestTypeMother.propioDeEmpresaDeshabilitado();

        assertThat(LaboratoryTestTypeDto.from(tipo).enabled()).isFalse();
    }
}
