package com.vetsoftware.app.vaccinationtype.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.vaccinationtype.domain.VaccinationType;
import com.vetsoftware.app.vaccinationtype.testsupport.VaccinationTypeMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("VaccinationTypeDto.from — mapeo campo por campo")
class VaccinationTypeDtoTest {

    @Test
    @DisplayName("copia cada campo de un tipo propio de una empresa")
    void copia_cada_campo_de_un_tipo_propio() {
        VaccinationType tipo = VaccinationTypeMother.propia();

        VaccinationTypeDto dto = VaccinationTypeDto.from(tipo);

        assertThat(dto.id()).isEqualTo(tipo.getId());
        assertThat(dto.name()).isEqualTo(tipo.getName());
        assertThat(dto.description()).isEqualTo(tipo.getDescription());
        assertThat(dto.company()).isEqualTo(CompanySummaryDto.from(tipo.getCompany()));
        assertThat(dto.general()).isFalse();
        assertThat(dto.createdDate()).isEqualTo(tipo.getCreatedDate());
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("un tipo general sin empresa mapea company a null")
    void un_tipo_general_sin_empresa_mapea_company_a_null() {
        VaccinationType tipo = VaccinationTypeMother.general();

        VaccinationTypeDto dto = VaccinationTypeDto.from(tipo);

        assertThat(dto.company()).isNull();
        assertThat(dto.general()).isTrue();
    }

    @Test
    @DisplayName("un tipo deshabilitado mapea enabled en falso")
    void un_tipo_deshabilitado_mapea_enabled_en_falso() {
        VaccinationType tipo = VaccinationTypeMother.deshabilitada();

        VaccinationTypeDto dto = VaccinationTypeDto.from(tipo);

        assertThat(dto.enabled()).isFalse();
    }
}
