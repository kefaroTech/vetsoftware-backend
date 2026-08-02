package com.vetsoftware.app.cashregister.application.dto;

import com.vetsoftware.app.cashregister.domain.CashPaymentMethod;
import com.vetsoftware.app.cashregister.domain.CashSessionCount;
import java.math.BigDecimal;

/**
 * Salida de un conteo de cierre por método: esperado vs contado y su diferencia (contado −
 * esperado).
 */
public record CashSessionCountView(
    CashPaymentMethod method,
    BigDecimal expectedAmount,
    BigDecimal countedAmount,
    BigDecimal difference) {

  public static CashSessionCountView from(CashSessionCount c) {
    return new CashSessionCountView(
        c.getMethod(), c.getExpectedAmount(), c.getCountedAmount(), c.difference());
  }
}
