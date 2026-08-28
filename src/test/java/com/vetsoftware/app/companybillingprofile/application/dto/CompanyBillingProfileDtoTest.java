package com.vetsoftware.app.companybillingprofile.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.companybillingprofile.domain.CompanyBillingProfile;
import com.vetsoftware.app.companybillingprofile.domain.PersonKind;
import com.vetsoftware.app.companybillingprofile.domain.TaxIdKind;
import com.vetsoftware.app.companybillingprofile.domain.TaxRegime;
import com.vetsoftware.app.companybillingprofile.testsupport.CompanyBillingProfileMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("CompanyBillingProfileDto")
class CompanyBillingProfileDtoTest {

    @Test
    @DisplayName("copia campo a campo la ficha de una sociedad sin cruzar ninguna columna")
    void copia_campo_a_campo_la_ficha_de_una_sociedad() {
        CompanyBillingProfile ficha = CompanyBillingProfileMother.persistida(42L);

        CompanyBillingProfileDto dto = CompanyBillingProfileDto.from(ficha);

        assertThat(dto.id()).isEqualTo(42L);
        assertThat(dto.companyId()).isEqualTo(CompanyBillingProfileMother.COMPANY_ID);
        assertThat(dto.personKind()).isEqualTo(PersonKind.LEGAL);
        assertThat(dto.taxIdKind()).isEqualTo(TaxIdKind.NIT);
        assertThat(dto.taxId()).isEqualTo(CompanyBillingProfileMother.NIT);
        assertThat(dto.verificationDigit())
                .isEqualTo(CompanyBillingProfileMother.DIGITO_VERIFICACION);
        assertThat(dto.legalName()).isEqualTo(CompanyBillingProfileMother.RAZON_SOCIAL);
        assertThat(dto.address()).isEqualTo(CompanyBillingProfileMother.DIRECCION);
        assertThat(dto.billingEmail()).isEqualTo(CompanyBillingProfileMother.CORREO);
        assertThat(dto.taxRegime()).isEqualTo(TaxRegime.COMMON);
        assertThat(dto.withholdingAgent()).isTrue();
        assertThat(dto.validFrom()).isEqualTo(CompanyBillingProfileMother.RIGE_DESDE);
        assertThat(dto.validTo()).isNull();
        assertThat(dto.createdDate()).isEqualTo(CompanyBillingProfileMother.CREADA_EL);
    }

    @Test
    @DisplayName("conserva los cuatro campos de nombre de la persona natural por separado")
    void conserva_los_cuatro_campos_de_nombre_por_separado() {
        // Si el DTO los juntara «para simplificar», habria que volver a partirlos para
        // la informacion exogena, que es exactamente lo que este modelo evita.
        CompanyBillingProfileDto dto = CompanyBillingProfileDto
                .from(CompanyBillingProfileMother.personaNatural());

        assertThat(dto.firstName()).isEqualTo(CompanyBillingProfileMother.PRIMER_NOMBRE);
        assertThat(dto.middleName()).isEqualTo(CompanyBillingProfileMother.OTROS_NOMBRES);
        assertThat(dto.lastName()).isEqualTo(CompanyBillingProfileMother.PRIMER_APELLIDO);
        assertThat(dto.secondLastName()).isEqualTo(CompanyBillingProfileMother.SEGUNDO_APELLIDO);
        assertThat(dto.legalName()).isNull();
    }

    @Test
    @DisplayName("el municipio viaja como CitySummaryDto y no como el value object del dominio")
    void el_municipio_viaja_como_summary() {
        CompanyBillingProfileDto dto = CompanyBillingProfileDto
                .from(CompanyBillingProfileMother.persistida(42L));

        assertThat(dto.city()).isEqualTo(new CitySummaryDto(900L, "Medellin"));
    }

    @Test
    @DisplayName("una ficha cerrada lleva su fecha de fin: es lo que la distingue de la vigente")
    void una_ficha_cerrada_lleva_su_fecha_de_fin() {
        CompanyBillingProfileDto dto = CompanyBillingProfileDto.from(
                CompanyBillingProfileMother.persistida(41L, CompanyBillingProfileMother.COMPANY_ID,
                        CompanyBillingProfileMother.RIGE_DESDE,
                        CompanyBillingProfileMother.SUCEDE_DESDE));

        assertThat(dto.validTo()).isEqualTo(CompanyBillingProfileMother.SUCEDE_DESDE);
    }
}
