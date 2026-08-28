package com.vetsoftware.app.supplierwithholding.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.supplierwithholding.domain.SupplierWithholding;
import com.vetsoftware.app.supplierwithholding.testsupport.SupplierWithholdingMother;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SupplierWithholdingDto")
class SupplierWithholdingDtoTest {

    @Test
    @DisplayName("traduce una retencion recien practicada campo a campo, sin certificar")
    void traduce_una_retencion_recien_practicada_campo_a_campo() {
        SupplierWithholding origen = SupplierWithholdingMother.ica();

        SupplierWithholdingDto dto = SupplierWithholdingDto.from(origen);

        assertThat(dto.id()).isEqualTo(origen.getId());
        assertThat(dto.supplierTaxId()).isEqualTo(origen.getSupplierTaxId());
        assertThat(dto.supplierName()).isEqualTo(origen.getSupplierName());
        assertThat(dto.supplierDocType()).isEqualTo(origen.getSupplierDocType());
        assertThat(dto.supplierInvoiceRef()).isEqualTo(origen.getSupplierInvoiceRef());
        assertThat(dto.withholdingType()).isEqualTo(origen.getWithholdingType());
        assertThat(dto.concept()).isEqualTo(origen.getConcept());
        assertThat(dto.taxableBase()).isEqualByComparingTo(origen.getTaxableBase());
        assertThat(dto.ratePercent()).isEqualByComparingTo(origen.getRatePercent());
        assertThat(dto.amount()).isEqualByComparingTo(origen.getAmount());
        assertThat(dto.municipalityCode()).isEqualTo(origen.getMunicipalityCode());
        assertThat(dto.fiscalYear()).isEqualTo(origen.getFiscalYear());
        assertThat(dto.fiscalPeriodKey()).isEqualTo(origen.getFiscalPeriodKey());
        assertThat(dto.practicedOn()).isEqualTo(origen.getPracticedOn());
        assertThat(dto.createdDate()).isEqualTo(origen.getCreatedDate());
        assertThat(dto.certificateIssuedAt()).isNull();
        assertThat(dto.certificateRef()).isNull();
        assertThat(dto.paymentReceiptRef()).isNull();
    }

    @Test
    @DisplayName("traduce una retencion ya certificada con sus dos campos de certificado")
    void traduce_una_retencion_ya_certificada() {
        LocalDateTime emitidoEl = LocalDateTime.of(2026, 4, 1, 9, 0);
        SupplierWithholding origen = SupplierWithholdingMother.conId(300L,
                SupplierWithholdingMother.conCertificado(emitidoEl, "CERT-2026-300"));

        SupplierWithholdingDto dto = SupplierWithholdingDto.from(origen);

        assertThat(dto.id()).isEqualTo(300L);
        assertThat(dto.certificateIssuedAt()).isEqualTo(origen.getCertificateIssuedAt());
        assertThat(dto.certificateRef()).isEqualTo(origen.getCertificateRef());
        assertThat(origen.isCertified()).isTrue();
    }
}
