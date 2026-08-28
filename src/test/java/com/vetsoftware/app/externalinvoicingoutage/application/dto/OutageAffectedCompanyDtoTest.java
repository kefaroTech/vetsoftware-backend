package com.vetsoftware.app.externalinvoicingoutage.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.externalinvoicingoutage.domain.ExternalInvoicingOutageCompany;
import com.vetsoftware.app.externalinvoicingoutage.domain.OutageResolution;
import com.vetsoftware.app.externalinvoicingoutage.testsupport.ExternalInvoicingOutageMother;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("OutageAffectedCompanyDto.from")
class OutageAffectedCompanyDtoTest {

    @Test
    @DisplayName("copia id, caida, empresa, documentos fallidos y resolucion sin cruzarlos")
    void copia_cada_campo_sin_cruzarlos() {
        OutageAffectedCompanyDto dto = OutageAffectedCompanyDto
                .from(ExternalInvoicingOutageMother.afectada());

        assertThat(dto.id()).isEqualTo(ExternalInvoicingOutageMother.AFFECTED_ID);
        assertThat(dto.outageId()).isEqualTo(ExternalInvoicingOutageMother.OUTAGE_ID);
        assertThat(dto.companyId()).isEqualTo(ExternalInvoicingOutageMother.COMPANY_ID);
        assertThat(dto.failedDocumentCount())
                .isEqualTo(ExternalInvoicingOutageMother.FAILED_DOCUMENT_COUNT);
        assertThat(dto.resolvedBy()).isEqualTo(ExternalInvoicingOutageMother.RESOLVED_BY);
    }

    @Test
    @DisplayName("contingencyNumbering es verdadero cuando resolvedBy es CONTINGENCY_NUMBERING")
    void contingency_numbering_verdadero_para_numeracion_de_contingencia() {
        OutageAffectedCompanyDto dto = OutageAffectedCompanyDto
                .from(ExternalInvoicingOutageMother.afectada());

        assertThat(dto.contingencyNumbering()).isTrue();
    }

    @Test
    @DisplayName("contingencyNumbering es falso para cualquier otra resolucion")
    void contingency_numbering_falso_para_otra_resolucion() {
        ExternalInvoicingOutageCompany resueltaPorReintento = new ExternalInvoicingOutageCompany(
                ExternalInvoicingOutageMother.AFFECTED_ID, ExternalInvoicingOutageMother.OUTAGE_ID,
                ExternalInvoicingOutageMother.COMPANY_ID,
                ExternalInvoicingOutageMother.FAILED_DOCUMENT_COUNT, OutageResolution.RETRIED);

        OutageAffectedCompanyDto dto = OutageAffectedCompanyDto.from(resueltaPorReintento);

        assertThat(dto.contingencyNumbering()).isFalse();
    }
}
