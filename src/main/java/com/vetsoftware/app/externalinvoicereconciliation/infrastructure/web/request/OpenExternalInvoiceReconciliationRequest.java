package com.vetsoftware.app.externalinvoicereconciliation.infrastructure.web.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/**
 * <strong>Sin {@code companyId}.</strong> La empresa del documento hace falta
 * -la FK contra {@code subscription_billing_documents} es compuesta- pero viaja
 * como {@code @RequestParam}, que es la forma que permite la regla dura
 * {@code EMPRESA_NO_VIAJA_EN_EL_CUERPO}: esa regla mira <em>todo</em>
 * {@code @RequestBody} sin mirar la ruta ni el rol.
 *
 * @param computedTotal
 *            el total que calculo Lumbre. {@code @PositiveOrZero} y no
 *            {@code @Positive}: un documento de cero es raro pero legitimo -una
 *            nota que se compensa entera- y rechazarlo en el binder impediria
 *            registrar su conciliacion
 * @param computedTax
 *            el impuesto propio, aparte del total a proposito
 */
public record OpenExternalInvoiceReconciliationRequest(
        @NotNull(message = "Debes indicar el documento de cobro que se concilia.") Long billingDocumentId,
        @NotNull(message = "El total calculado es obligatorio.") @PositiveOrZero(message = "El total calculado no puede ser negativo.") BigDecimal computedTotal,
        @NotNull(message = "El impuesto calculado es obligatorio.") @PositiveOrZero(message = "El impuesto calculado no puede ser negativo.") BigDecimal computedTax) {
}
