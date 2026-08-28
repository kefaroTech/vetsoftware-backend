package com.vetsoftware.app.paymentreversal.application.command;

import com.vetsoftware.app.paymentreversal.domain.ReversalOutcome;
import java.math.BigDecimal;

/**
 * @param appliedAmount
 *            obligatorio y positivo cuando el desenlace mueve dinero; prohibido
 *            cuando no lo mueve
 * @param resultingRefundId
 *            la devolucion que materializo la reversion, si ya existe. Se
 *            valida acotada por empresa
 */
public record ResolveReversalRequestCommand(Long id, Long companyId, ReversalOutcome outcome,
        BigDecimal appliedAmount, Long resultingRefundId) {
}
