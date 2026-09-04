package com.vetsoftware.app.externalinvoicereconciliation.application.command;

import java.math.BigDecimal;

/**
 * Abre la ficha de conciliacion de un documento de cobro ya devengado.
 *
 * @param companyId
 *            la empresa del documento de cobro. Viaja en el command porque la
 *            FK contra {@code subscription_billing_documents} es COMPUESTA
 *            {@code (company_id, billing_document_id)}: sin la empresa, el
 *            {@code billingDocumentId} no identifica una fila. No viaja en el
 *            cuerpo HTTP -lo prohibe {@code EMPRESA_NO_VIAJA_EN_EL_CUERPO}-
 *            sino como {@code @RequestParam}, y el puerto esta cerrado a
 *            {@code hasRole('SYSTEM')} a secas
 * @param computedTotal
 *            el total que calculo Lumbre, <strong>una vez sobre la base
 *            agregada</strong>
 * @param computedTax
 *            el impuesto propio, aparte del total a proposito: sin separarlos
 *            no se puede saber si un descuadre futuro es de base o de calculo
 */
public record OpenExternalInvoiceReconciliationCommand(Long companyId, Long billingDocumentId,
        BigDecimal computedTotal, BigDecimal computedTax) {
}
