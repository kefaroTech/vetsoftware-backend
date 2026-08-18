package com.vetsoftware.app.numberingresolution.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.numberingresolution.domain.NumberingResolution;
import com.vetsoftware.app.numberingresolution.testsupport.NumberingResolutionMother;
import java.lang.reflect.Field;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("NumberingResolutionDto")
class NumberingResolutionDtoTest {

    @Test
    @DisplayName("from() copia cada campo de la resolucion, incluida la empresa")
    void from_copia_cada_campo() {
        NumberingResolution resolucion = NumberingResolutionMother.activaDeEmpresa();

        NumberingResolutionDto dto = NumberingResolutionDto.from(resolucion);

        assertThat(dto.id()).isEqualTo(resolucion.getId());
        assertThat(dto.company().id()).isEqualTo(NumberingResolutionMother.COMPANY_ID);
        assertThat(dto.company().name()).isEqualTo("Veterinaria Central");
        assertThat(dto.company().identifier()).isEqualTo("900123456");
        assertThat(dto.branchId()).isNull();
        assertThat(dto.documentType()).isEqualTo(resolucion.getDocumentType());
        assertThat(dto.resolutionNumber()).isEqualTo(resolucion.getResolutionNumber());
        assertThat(dto.resolutionDate()).isEqualTo(resolucion.getResolutionDate());
        assertThat(dto.prefix()).isEqualTo(resolucion.getPrefix());
        assertThat(dto.rangeFrom()).isEqualTo(resolucion.getRangeFrom());
        assertThat(dto.rangeTo()).isEqualTo(resolucion.getRangeTo());
        assertThat(dto.validFrom()).isEqualTo(resolucion.getValidFrom());
        assertThat(dto.validTo()).isEqualTo(resolucion.getValidTo());
        assertThat(dto.technicalKey()).isEqualTo(resolucion.getTechnicalKey());
        assertThat(dto.currentNumber()).isEqualTo(resolucion.getCurrentNumber());
        assertThat(dto.createdDate()).isEqualTo(resolucion.getCreatedDate());
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("from() deja la empresa en null si la resolucion no trae una")
    void from_deja_la_empresa_en_null_si_no_hay_referencia() throws Exception {
        NumberingResolution resolucion = NumberingResolutionMother.activaDeEmpresa();
        // El constructor de NumberingResolution exige company != null (validate()), asi
        // que en produccion esta rama del mapper es defensiva y nunca se alcanza por la
        // API publica. Se fuerza por reflexion para dejar constancia de que from() no
        // revienta si algun dia esa invariante se relaja.
        Field companyField = NumberingResolution.class.getDeclaredField("company");
        companyField.setAccessible(true);
        companyField.set(resolucion, null);

        assertThat(NumberingResolutionDto.from(resolucion).company()).isNull();
    }
}
