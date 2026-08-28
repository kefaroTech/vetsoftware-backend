package com.vetsoftware.app.supplierwithholding.testsupport;

import com.vetsoftware.app.supplierwithholding.domain.SupplierDocumentKind;
import com.vetsoftware.app.supplierwithholding.domain.SupplierWithholding;
import com.vetsoftware.app.supplierwithholding.domain.SupplierWithholdingType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Fixtures de {@link SupplierWithholding}.
 *
 * <p>
 * La entidad se construye de verdad. El constructor crudo se expone porque es
 * el que usa {@code SupplierWithholdingJpaMapper} al reconstruir una fila, y
 * porque varias comprobaciones solo son observables con una combinacion que
 * ninguna factoria produce.
 */
public final class SupplierWithholdingMother {

    public static final int ANIO = 2026;
    public static final String MES = "2026-M03";
    public static final String BIMESTRE = "2026-B02";
    public static final String MUNICIPIO = "05001";
    public static final LocalDate PRACTICADA_EL = LocalDate.of(2026, 3, 18);
    public static final LocalDateTime CREADA_EL = LocalDateTime.of(2026, 3, 18, 11, 30);

    private SupplierWithholdingMother() {
    }

    /** Retencion en la fuente de renta, mensual y nacional. El caso corriente. */
    public static SupplierWithholding renta() {
        return SupplierWithholding.practice("900123456", "Distribuidora Veterinaria SAS",
                SupplierDocumentKind.NIT, "FV-2026-8891", SupplierWithholdingType.INCOME_TAX,
                "Compra de medicamentos", new BigDecimal("2500000.00"), new BigDecimal("2.500000"),
                new BigDecimal("62500.00"), null, ANIO, MES, PRACTICADA_EL, CREADA_EL);
    }

    /** ReteICA de Medellin, bimestral y con municipio. */
    public static SupplierWithholding ica() {
        return SupplierWithholding.practice("900123456", "Distribuidora Veterinaria SAS",
                SupplierDocumentKind.NIT, "FV-2026-8891", SupplierWithholdingType.ICA,
                "Compra de medicamentos", new BigDecimal("2500000.00"), new BigDecimal("0.700000"),
                new BigDecimal("17500.00"), MUNICIPIO, ANIO, BIMESTRE, PRACTICADA_EL, CREADA_EL);
    }

    /** ReteIVA, bimestral y nacional. */
    public static SupplierWithholding iva() {
        return SupplierWithholding.practice("900123456", "Distribuidora Veterinaria SAS",
                SupplierDocumentKind.NIT, "FV-2026-8891", SupplierWithholdingType.VAT,
                "Compra de medicamentos", new BigDecimal("2500000.00"), new BigDecimal("15.000000"),
                new BigDecimal("375000.00"), null, ANIO, BIMESTRE, PRACTICADA_EL, CREADA_EL);
    }

    /** La clave de periodo que le corresponde a cada clase de retencion. */
    public static String periodoValido(SupplierWithholdingType tipo) {
        return tipo == SupplierWithholdingType.INCOME_TAX ? MES : BIMESTRE;
    }

    /** El municipio que le corresponde a cada clase: solo ICA lo lleva. */
    public static String municipioValido(SupplierWithholdingType tipo) {
        return tipo == SupplierWithholdingType.ICA ? MUNICIPIO : null;
    }

    /** Reconstruye con un {@code id}, como hace el mapper al leer de la base. */
    public static SupplierWithholding conId(Long id, SupplierWithholding origen) {
        return new SupplierWithholding(id, origen.getSupplierTaxId(), origen.getSupplierName(),
                origen.getSupplierDocType(), origen.getSupplierInvoiceRef(),
                origen.getWithholdingType(), origen.getConcept(), origen.getTaxableBase(),
                origen.getRatePercent(), origen.getAmount(), origen.getMunicipalityCode(),
                origen.getFiscalYear(), origen.getFiscalPeriodKey(), origen.getPracticedOn(),
                origen.getCertificateIssuedAt(), origen.getCertificateRef(),
                origen.getPaymentReceiptRef(), origen.getCreatedDate(), origen.getVersion());
    }

    /**
     * El constructor crudo con los valores de una retencion valida, para que cada
     * caso cambie solo lo que quiere romper.
     */
    @SuppressWarnings("checkstyle:ParameterNumber")
    public static SupplierWithholding crudo(String supplierTaxId, String supplierName,
            SupplierDocumentKind docType, String invoiceRef, SupplierWithholdingType tipo,
            String concept, BigDecimal taxableBase, BigDecimal ratePercent, BigDecimal amount,
            String municipalityCode, int fiscalYear, String fiscalPeriodKey, LocalDate practicedOn,
            LocalDateTime certificateIssuedAt, String certificateRef, String paymentReceiptRef,
            LocalDateTime createdDate) {
        return new SupplierWithholding(null, supplierTaxId, supplierName, docType, invoiceRef, tipo,
                concept, taxableBase, ratePercent, amount, municipalityCode, fiscalYear,
                fiscalPeriodKey, practicedOn, certificateIssuedAt, certificateRef,
                paymentReceiptRef, createdDate, null);
    }

    /** Un crudo valido salvo por el certificado, que el caso decide. */
    public static SupplierWithholding conCertificado(LocalDateTime emitidoEl, String referencia) {
        return crudo("900123456", "Distribuidora Veterinaria SAS", SupplierDocumentKind.NIT,
                "FV-2026-8891", SupplierWithholdingType.INCOME_TAX, "Compra de medicamentos",
                new BigDecimal("2500000.00"), new BigDecimal("2.500000"),
                new BigDecimal("62500.00"), null, ANIO, MES, PRACTICADA_EL, emitidoEl, referencia,
                null, CREADA_EL);
    }
}
