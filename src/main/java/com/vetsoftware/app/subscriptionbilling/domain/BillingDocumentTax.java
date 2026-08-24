package com.vetsoftware.app.subscriptionbilling.domain;

import com.vetsoftware.app.shared.domain.Money;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Una línea del desglose fiscal: la base agregada de todas las líneas del
 * documento que comparten tratamiento y tarifa, y el impuesto calculado sobre
 * ella.
 *
 * <p>
 * <b>Es el único sitio donde el IVA está calculado una sola vez y de la forma
 * correcta.</b> El cargo guarda su base y su tarifa, nunca su impuesto; el
 * impuesto se calcula aquí, sobre la base <b>agregada</b>. Calcularlo por línea
 * y sumarlo después da un resultado distinto por el redondeo, y esa diferencia
 * de un peso por documento es exactamente lo que descuadra la declaración
 * bimestral.
 *
 * <p>
 * {@code uq_sbdt_document_rate} sobre
 * {@code (billing_document_id, tax_treatment, tax_rate)} es lo que hace cumplir
 * el «una sola vez»: si aparecieran dos bloques con el mismo tratamiento y la
 * misma tarifa, la base declarada sería el doble.
 *
 * <p>
 * <b>Sin {@code version} ({@code E1_APPEND_ONLY}) y sin {@code enabled}</b>: es
 * la base declarable, se calcula una vez al cerrar el documento y el bloqueo
 * vive en la cabecera, que sí va versionada. Ocultar una fila cambiaría lo
 * declarado sin dejar rastro.
 */
public record BillingDocumentTax(Long id, Long companyId, Long billingDocumentId,
        TaxTreatment taxTreatment, BigDecimal taxRate, BigDecimal taxableBase, BigDecimal taxAmount,
        LocalDateTime createdDate) {

    public BillingDocumentTax {
        if (companyId == null)
            throw new IllegalArgumentException("companyId is required");
        if (taxTreatment == null)
            throw new IllegalArgumentException("taxTreatment is required");
        taxTreatment.validarTarifa(taxRate);
        if (taxableBase == null || taxableBase.signum() < 0)
            throw new IllegalArgumentException("taxableBase cannot be negative");
        if (taxAmount == null || taxAmount.signum() < 0)
            throw new IllegalArgumentException("taxAmount cannot be negative");
        if (taxTreatment != TaxTreatment.TAXED && taxAmount.signum() != 0)
            throw new IllegalArgumentException(taxTreatment + " requires a zero tax amount");
    }

    /**
     * Calcula la línea desde la base agregada del grupo.
     *
     * <p>
     * La base entra ya en <b>valor absoluto</b>: el documento es siempre positivo y
     * el signo lo da su {@code document_kind}. Esa es la traducción entre las dos
     * convenciones, y {@link TaxBreakdown} es quien comprueba antes que el grupo no
     * mezcla signos de forma que el valor absoluto pierda información.
     */
    public static BillingDocumentTax of(Long companyId, TaxTreatment taxTreatment,
            BigDecimal taxRate, BigDecimal aggregatedBase, LocalDateTime createdDate) {
        BigDecimal base = Money.scaled(aggregatedBase.abs());
        BigDecimal impuesto = taxTreatment == TaxTreatment.TAXED
                ? Money.percentOf(base, taxRate)
                : Money.zero();
        return new BillingDocumentTax(null, companyId, null, taxTreatment, taxRate, base, impuesto,
                createdDate);
    }

    /**
     * La misma línea ya atada a su documento, tras conocerse el id de la cabecera.
     */
    public BillingDocumentTax attachedTo(Long documentId) {
        return new BillingDocumentTax(id, companyId, documentId, taxTreatment, taxRate, taxableBase,
                taxAmount, createdDate);
    }
}
