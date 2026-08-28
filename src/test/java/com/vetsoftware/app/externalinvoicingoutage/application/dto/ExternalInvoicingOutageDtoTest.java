package com.vetsoftware.app.externalinvoicingoutage.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.externalinvoicingoutage.testsupport.ExternalInvoicingOutageMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ExternalInvoicingOutageDto.from")
class ExternalInvoicingOutageDtoTest {

    @Test
    @DisplayName("copia cada campo de la caida cerrada en su posicion")
    void copia_cada_campo_de_la_caida_cerrada() {
        ExternalInvoicingOutageDto dto = ExternalInvoicingOutageDto
                .from(ExternalInvoicingOutageMother.cerrada());

        assertThat(dto.id()).isEqualTo(ExternalInvoicingOutageMother.OUTAGE_ID);
        assertThat(dto.startedAt()).isEqualTo(ExternalInvoicingOutageMother.STARTED_AT);
        assertThat(dto.endedAt()).isEqualTo(ExternalInvoicingOutageMother.ENDED_AT);
        assertThat(dto.causeParty()).isEqualTo(ExternalInvoicingOutageMother.CAUSE_PARTY);
        assertThat(dto.summary()).isEqualTo(ExternalInvoicingOutageMother.SUMMARY);
        assertThat(dto.affectedCompanyCount())
                .isEqualTo(ExternalInvoicingOutageMother.NOTIFIED_COMPANY_COUNT);
        assertThat(dto.notifiedCompaniesAt())
                .isEqualTo(ExternalInvoicingOutageMother.NOTIFIED_COMPANIES_AT);
        assertThat(dto.externalIncidentRef())
                .isEqualTo(ExternalInvoicingOutageMother.EXTERNAL_INCIDENT_REF);
        assertThat(dto.createdDate()).isEqualTo(ExternalInvoicingOutageMother.CREATED_DATE);
    }

    @Test
    @DisplayName("open refleja isOpen(): verdadero mientras la caida sigue viva")
    void open_es_verdadero_mientras_la_caida_sigue_viva() {
        ExternalInvoicingOutageDto dto = ExternalInvoicingOutageDto
                .from(ExternalInvoicingOutageMother.abierta());

        assertThat(dto.open()).isTrue();
        assertThat(dto.endedAt()).isNull();
    }

    @Test
    @DisplayName("open es falso una vez que la caida se cerro")
    void open_es_falso_una_vez_cerrada() {
        ExternalInvoicingOutageDto dto = ExternalInvoicingOutageDto
                .from(ExternalInvoicingOutageMother.cerrada());

        assertThat(dto.open()).isFalse();
    }
}
