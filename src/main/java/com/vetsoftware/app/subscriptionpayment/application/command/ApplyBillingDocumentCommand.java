package com.vetsoftware.app.subscriptionpayment.application.command;

import com.vetsoftware.app.subscriptionpayment.domain.ApplicationSourceKind;
import java.math.BigDecimal;

/**
 * Aplicar un origen contra una factura. {@code paymentId} y
 * {@code sourceDocumentId} son excluyentes: los valida la entidad de dominio y
 * los vuelve a validar {@code chk_bda_source_exclusive} en la base.
 *
 * @param clientRequestId
 *            llave de idempotencia (R13). Sin ella, un doble clic o un
 *            reintento del navegador salda la factura dos veces, y R3 no lo
 *            detiene porque mide el total aplicado desde el origen, no cuantas
 *            veces se aplico
 */
public record ApplyBillingDocumentCommand(Long companyId, Long targetDocumentId,
        ApplicationSourceKind sourceKind, Long paymentId, Long sourceDocumentId,
        BigDecimal appliedAmount, String clientRequestId) {
}
