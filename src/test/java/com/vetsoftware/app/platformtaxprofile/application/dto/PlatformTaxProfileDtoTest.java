package com.vetsoftware.app.platformtaxprofile.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.platformtaxprofile.domain.PlatformTaxProfile;
import com.vetsoftware.app.platformtaxprofile.testsupport.PlatformTaxProfileMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PlatformTaxProfileDto")
class PlatformTaxProfileDtoTest {

    @Test
    @DisplayName("from copia cada campo, incluida la actividad economica resumida")
    void from_copia_cada_campo_incluida_la_actividad() {
        PlatformTaxProfile perfil = PlatformTaxProfileMother.vigente();

        PlatformTaxProfileDto dto = PlatformTaxProfileDto.from(perfil);

        assertThat(dto.id()).isEqualTo(perfil.getId());
        assertThat(dto.documentType()).isEqualTo(perfil.getDocumentType());
        assertThat(dto.documentId()).isEqualTo(perfil.getDocumentId());
        assertThat(dto.verificationDigit()).isEqualTo(perfil.getVerificationDigit());
        assertThat(dto.legalName()).isEqualTo(perfil.getLegalName());
        assertThat(dto.taxRegime()).isEqualTo(perfil.getTaxRegime());
        assertThat(dto.fiscalEmail()).isEqualTo(perfil.getFiscalEmail());
        assertThat(dto.commercialName()).isEqualTo(perfil.getCommercialName());
        assertThat(dto.selfWithholder()).isEqualTo(perfil.isSelfWithholder());
        assertThat(dto.validFrom()).isEqualTo(perfil.getValidFrom());
        assertThat(dto.validTo()).isEqualTo(perfil.getValidTo());
        assertThat(dto.createdDate()).isEqualTo(perfil.getCreatedDate());
        assertThat(dto.economicActivity()).isEqualTo(new PlatformEconomicActivitySummaryDto(
                PlatformTaxProfileMother.ACTIVIDAD.id(), PlatformTaxProfileMother.ACTIVIDAD.code(),
                PlatformTaxProfileMother.ACTIVIDAD.name()));
    }

    @Test
    @DisplayName("sin actividad economica el resumen sale nulo, no vacio")
    void sin_actividad_economica_el_resumen_sale_nulo() {
        PlatformTaxProfile perfil = PlatformTaxProfileMother.vigenteSinActividad();

        PlatformTaxProfileDto dto = PlatformTaxProfileDto.from(perfil);

        assertThat(dto.economicActivity()).isNull();
    }
}
