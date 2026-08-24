package com.vetsoftware.app.company.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.company.domain.Company;
import com.vetsoftware.app.company.testsupport.CompanyMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * El mapeo campo a campo se prueba entero a proposito: un intercambio entre dos
 * campos del mismo tipo —{@code name}/{@code identifier},
 * {@code address}/{@code contactNumber}— compila, pasa cualquier test de "no es
 * null", y solo se ve en pantalla.
 */
@DisplayName("CompanyDto.from")
class CompanyDtoTest {

    @Test
    @DisplayName("copia cada campo del agregado en su posicion")
    void copia_cada_campo_del_agregado_en_su_posicion() {
        CompanyDto dto = CompanyDto.from(CompanyMother.clinicaNorte());

        assertThat(dto.id()).isEqualTo(CompanyMother.COMPANY_ID);
        assertThat(dto.name()).isEqualTo("Clinica Norte");
        assertThat(dto.identifier()).isEqualTo("NIT-900");
        assertThat(dto.address()).isEqualTo("Calle 123 #45-67");
        assertThat(dto.contactNumber()).isEqualTo("3001234567");
        assertThat(dto.createdDate()).isEqualTo(CompanyMother.CREADO);
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("aplana el companion VO de ciudad en su summary sin perder campos")
    void aplana_los_companion_vo_en_summaries() {
        CompanyDto dto = CompanyDto.from(CompanyMother.clinicaNorte());

        assertThat(dto.city()).isEqualTo(
                new CitySummaryDto(CompanyMother.BOGOTA.id(), CompanyMother.BOGOTA.name()));
    }

    @Test
    @DisplayName("propaga los campos opcionales nulos sin sustituirlos")
    void propaga_los_campos_opcionales_nulos() {
        CompanyDto dto = CompanyDto.from(CompanyMother.sinDatosOpcionales());

        assertThat(dto.address()).isNull();
        assertThat(dto.contactNumber()).isNull();
    }

    @Test
    @DisplayName("propaga la empresa deshabilitada")
    void propaga_la_empresa_deshabilitada() {
        assertThat(CompanyDto.from(CompanyMother.deshabilitada()).enabled()).isFalse();
    }

    @Test
    @DisplayName("propaga id nulo de la empresa aun no persistida")
    void propaga_id_nulo() {
        Company sinPersistir = Company.create("Clinica Norte", "NIT-900", null, null,
                CompanyMother.BOGOTA, CompanyMother.CREADO);

        assertThat(CompanyDto.from(sinPersistir).id()).isNull();
    }
}
