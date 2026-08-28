package com.vetsoftware.app.supplierwithholding.application.dto;

import com.vetsoftware.app.supplierwithholding.domain.SupplierDocumentKind;
import com.vetsoftware.app.supplierwithholding.domain.SupplierWithholding;
import com.vetsoftware.app.supplierwithholding.domain.SupplierWithholdingType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** <strong>Sin {@code version}</strong>: es una barandilla del que escribe. */
public record SupplierWithholdingDto(Long id, String supplierTaxId, String supplierName,
        SupplierDocumentKind supplierDocType, String supplierInvoiceRef,
        SupplierWithholdingType withholdingType, String concept, BigDecimal taxableBase,
        BigDecimal ratePercent, BigDecimal amount, String municipalityCode, int fiscalYear,
        String fiscalPeriodKey, LocalDate practicedOn, LocalDateTime certificateIssuedAt,
        String certificateRef, String paymentReceiptRef, LocalDateTime createdDate) {

    public static SupplierWithholdingDto from(SupplierWithholding withholding) {
        return new SupplierWithholdingDto(withholding.getId(), withholding.getSupplierTaxId(),
                withholding.getSupplierName(), withholding.getSupplierDocType(),
                withholding.getSupplierInvoiceRef(), withholding.getWithholdingType(),
                withholding.getConcept(), withholding.getTaxableBase(),
                withholding.getRatePercent(), withholding.getAmount(),
                withholding.getMunicipalityCode(), withholding.getFiscalYear(),
                withholding.getFiscalPeriodKey(), withholding.getPracticedOn(),
                withholding.getCertificateIssuedAt(), withholding.getCertificateRef(),
                withholding.getPaymentReceiptRef(), withholding.getCreatedDate());
    }
}
