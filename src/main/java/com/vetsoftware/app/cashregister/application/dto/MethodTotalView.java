package com.vetsoftware.app.cashregister.application.dto;

import com.vetsoftware.app.cashregister.domain.CashPaymentMethod;
import java.math.BigDecimal;

/** Total esperado (en vivo) de un método = Σ movimientos con signo, más la base para el EFECTIVO. */
public record MethodTotalView(CashPaymentMethod method, BigDecimal expectedAmount) {}
