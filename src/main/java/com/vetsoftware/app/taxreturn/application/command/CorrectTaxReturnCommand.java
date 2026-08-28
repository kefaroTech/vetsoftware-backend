package com.vetsoftware.app.taxreturn.application.command;

import java.math.BigDecimal;

/**
 * Abrir la correccion de una declaracion ya presentada.
 *
 * <p>
 * <strong>Una correccion es una declaracion nueva del mismo periodo, no una
 * edicion.</strong> Nace con {@code sequenceNumber + 1}, apuntando a la que
 * corrige, y obliga a que la anterior pase a {@code CORRECTED} — porque
 * mientras siga {@code FILED}, {@code uq_tax_returns_current} impide que exista
 * la nueva.
 *
 * @param id
 *            la declaracion que se corrige, no la que se crea
 */
public record CorrectTaxReturnCommand(Long id, BigDecimal totalGenerated,
        BigDecimal totalDeductible, BigDecimal balancePayable, BigDecimal balanceCredit) {
}
