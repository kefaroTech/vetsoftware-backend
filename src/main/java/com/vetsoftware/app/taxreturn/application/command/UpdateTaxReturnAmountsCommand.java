package com.vetsoftware.app.taxreturn.application.command;

import java.math.BigDecimal;

/**
 * Corregir los importes de un borrador.
 *
 * <p>
 * <strong>No lleva ni impuesto, ni año, ni periodo, ni municipio</strong>: los
 * cuatro definen <em>que</em> declaracion es esta y cambiarlos la convertiria
 * en otra. Si el supuesto estaba mal, se anula el borrador y se abre el
 * correcto.
 *
 * <p>
 * Solo aplica en {@code DRAFT}: una declaracion presentada no se edita, se
 * sucede.
 */
public record UpdateTaxReturnAmountsCommand(Long id, BigDecimal totalGenerated,
        BigDecimal totalDeductible, BigDecimal balancePayable, BigDecimal balanceCredit) {
}
