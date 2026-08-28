package com.vetsoftware.app.subscriptionpayment.domain;

import java.math.BigDecimal;

/**
 * Companion VO de la retencion que vive en {@code documentwithholding}.
 *
 * <p>
 * <b>Solo tres datos, y ninguno es fiscal.</b> El tipo, la base, la tarifa, el
 * municipio, el periodo gravable y el certificado <b>no se copian aqui</b>:
 * viven en {@code document_withholdings} y duplicarlos crearia dos verdades
 * sobre una cifra que se declara ante la DIAN. Lo que esta rodaja necesita
 * saber es cuanto salda, de quien es y contra que documento se practico.
 *
 * @param billingDocumentId
 *            la factura sobre la que se practico. <b>Es lo que impide aplicar
 *            la retencion de una factura contra otra</b>: sin esta comprobacion
 *            se podria saldar la factura de septiembre con la retencion de la
 *            de agosto, y la cartera cuadraria mientras la declaracion no
 */
public record WithholdingRef(Long id, Long companyId, Long billingDocumentId, BigDecimal amount) {

    public WithholdingRef {
        if (id == null)
            throw new IllegalArgumentException("withholding id is required");
        if (companyId == null)
            throw new IllegalArgumentException("withholding companyId is required");
        if (billingDocumentId == null)
            throw new IllegalArgumentException("withholding billingDocumentId is required");
        if (amount == null || amount.signum() <= 0)
            throw new IllegalArgumentException("withholding amount must be greater than zero");
    }

    /** {@code true} si la retencion se practico sobre ese documento. */
    public boolean esDelDocumento(Long documentId) {
        return billingDocumentId.equals(documentId);
    }
}
