package com.vetsoftware.app.productchargeopenaccount.application.port.out;

import java.math.BigDecimal;

/**
 * Totales derivados de la Open Account padre, necesarios para validar invariantes
 * de saldo al mutar cargos (p.ej. no dejar los abonos por encima del total de cargos).
 * Lo implementa un adapter que delega en el cálculo centralizado de la feature openaccount.
 */
public interface OpenAccountTotalsQueryPort {
    /** Suma de cargos activos (producto + servicio + general) de la cuenta. */
    BigDecimal totalCharges(Long openAccountId);

    /** Suma de abonos activos de la cuenta. */
    BigDecimal totalPayments(Long openAccountId);
}
