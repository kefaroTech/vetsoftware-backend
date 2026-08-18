package com.vetsoftware.app.companytaxprofile.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.companytaxprofile.domain.CompanyDocumentType;
import com.vetsoftware.app.companytaxprofile.domain.CompanyTaxProfile;
import com.vetsoftware.app.companytaxprofile.domain.TaxRegime;
import com.vetsoftware.app.companytaxprofile.testsupport.CompanyTaxProfileMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * El mapeo campo a campo se prueba entero a proposito: un intercambio entre
 * {@code legalName}/{@code commercialName} compila, pasa cualquier test de "no
 * es null" y solo se ve al llegar a la factura electronica.
 */
@DisplayName("CompanyTaxProfileDto.from")
class CompanyTaxProfileDtoTest {

    @Test
    @DisplayName("copia cada campo del perfil, aplana la empresa y la actividad en summaries")
    void copia_cada_campo_del_perfil() {
        CompanyTaxProfile profile = CompanyTaxProfileMother.perfilNit();

        CompanyTaxProfileDto dto = CompanyTaxProfileDto.from(profile);

        assertThat(dto.id()).isEqualTo(CompanyTaxProfileMother.PROFILE_ID);
        assertThat(dto.company()).isEqualTo(new CompanySummaryDto(
                CompanyTaxProfileMother.CLINICA.id(), CompanyTaxProfileMother.CLINICA.name(),
                CompanyTaxProfileMother.CLINICA.identifier()));
        assertThat(dto.companyDocumentType()).isEqualTo(CompanyDocumentType.NIT);
        assertThat(dto.companyDocumentId()).isEqualTo(CompanyTaxProfileMother.NIT);
        assertThat(dto.companyDocumentVerificationDigit())
                .isEqualTo(CompanyTaxProfileMother.NIT_DV);
        assertThat(dto.legalName()).isEqualTo(CompanyTaxProfileMother.RAZON_SOCIAL);
        assertThat(dto.taxRegime()).isEqualTo(TaxRegime.RESPONSABLE_IVA);
        assertThat(dto.fiscalEmail()).isEqualTo(CompanyTaxProfileMother.EMAIL_FISCAL);
        assertThat(dto.commercialName()).isEqualTo(CompanyTaxProfileMother.NOMBRE_COMERCIAL);
        assertThat(dto.economicActivity())
                .isEqualTo(new EconomicActivitySummaryDto(CompanyTaxProfileMother.VETERINARIA.id(),
                        CompanyTaxProfileMother.VETERINARIA.code(),
                        CompanyTaxProfileMother.VETERINARIA.name()));
        assertThat(dto.responsibilities()).containsExactly("O-13", "O-15");
        assertThat(dto.createdDate()).isEqualTo(CompanyTaxProfileMother.CREADO);
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("sin actividad economica, economicActivity es null y responsibilities vacio")
    void sin_actividad_economica_deja_el_summary_null() {
        CompanyTaxProfileDto dto = CompanyTaxProfileDto
                .from(CompanyTaxProfileMother.perfilCedula());

        assertThat(dto.economicActivity()).isNull();
        assertThat(dto.responsibilities()).isEmpty();
        assertThat(dto.companyDocumentVerificationDigit()).isNull();
    }

    @Test
    @DisplayName("propaga el perfil deshabilitado")
    void propaga_el_perfil_deshabilitado() {
        CompanyTaxProfileDto dto = CompanyTaxProfileDto
                .from(CompanyTaxProfileMother.perfilDeshabilitado());

        assertThat(dto.enabled()).isFalse();
    }
}
