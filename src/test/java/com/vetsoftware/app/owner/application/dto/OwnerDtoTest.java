package com.vetsoftware.app.owner.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.owner.domain.FiscalResponsibility;
import com.vetsoftware.app.owner.domain.OwnerDocumentType;
import com.vetsoftware.app.owner.domain.PersonType;
import com.vetsoftware.app.owner.domain.TaxRegime;
import com.vetsoftware.app.owner.testsupport.OwnerMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * El mapeo campo a campo se prueba entero a proposito: un intercambio entre dos
 * campos del mismo tipo (name/legalName, taxRegime/fiscalResponsibility)
 * compila, pasa un test de "no es null" y solo se ve en pantalla.
 */
@DisplayName("OwnerDto.from")
class OwnerDtoTest {

    @Test
    @DisplayName("copia cada campo de una persona natural en su posicion")
    void copia_cada_campo_de_persona_natural_en_su_posicion() {
        OwnerDto dto = OwnerDto.from(OwnerMother.personaNatural());

        assertThat(dto.id()).isEqualTo(OwnerMother.OWNER_ID);
        assertThat(dto.name()).isEqualTo("Ana Ruiz");
        assertThat(dto.email()).isEqualTo("ana@vet.com");
        assertThat(dto.document()).isEqualTo("1020304050");
        assertThat(dto.documentType()).isEqualTo(OwnerDocumentType.CEDULA_CIUDADANIA);
        assertThat(dto.personType()).isEqualTo(PersonType.NATURAL);
        assertThat(dto.verificationDigit()).isNull();
        assertThat(dto.legalName()).isNull();
        assertThat(dto.address()).isEqualTo("Calle 1 # 2-3");
        assertThat(dto.phone()).isEqualTo("3001112233");
        assertThat(dto.withholdingAgent()).isFalse();
        assertThat(dto.taxRegime()).isEqualTo(TaxRegime.NO_RESPONSABLE_IVA);
        assertThat(dto.fiscalResponsibility()).isEqualTo(FiscalResponsibility.NO_APLICA);
        assertThat(dto.createdDate()).isEqualTo(OwnerMother.CREADO);
        assertThat(dto.enabled()).isTrue();
    }

    @Test
    @DisplayName("copia digito de verificacion y razon social de una persona juridica")
    void copia_digito_de_verificacion_y_razon_social_de_persona_juridica() {
        OwnerDto dto = OwnerDto.from(OwnerMother.personaJuridica());

        assertThat(dto.documentType()).isEqualTo(OwnerDocumentType.NIT);
        assertThat(dto.personType()).isEqualTo(PersonType.JURIDICA);
        assertThat(dto.verificationDigit()).isEqualTo("7");
        assertThat(dto.legalName()).isEqualTo("Veterinaria Sur S.A.S.");
        assertThat(dto.withholdingAgent()).isTrue();
        assertThat(dto.taxRegime()).isEqualTo(TaxRegime.RESPONSABLE_IVA);
        assertThat(dto.fiscalResponsibility()).isEqualTo(FiscalResponsibility.GRAN_CONTRIBUYENTE);
    }

    @Test
    @DisplayName("aplana los companion VO city y company en summaries sin perder campos")
    void aplana_los_companion_vo_en_summaries() {
        OwnerDto dto = OwnerDto.from(OwnerMother.personaNatural());

        assertThat(dto.city())
                .isEqualTo(new CitySummaryDto(OwnerMother.BOGOTA.id(), OwnerMother.BOGOTA.name()));
        assertThat(dto.company()).isEqualTo(new CompanySummaryDto(OwnerMother.CLINICA.id(),
                OwnerMother.CLINICA.name(), OwnerMother.CLINICA.identifier()));
    }

    @Test
    @DisplayName("propaga el owner deshabilitado")
    void propaga_el_owner_deshabilitado() {
        assertThat(OwnerDto.from(OwnerMother.deshabilitado()).enabled()).isFalse();
    }
}
