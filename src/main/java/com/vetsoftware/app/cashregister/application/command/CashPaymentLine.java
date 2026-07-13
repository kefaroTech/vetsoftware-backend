package com.vetsoftware.app.cashregister.application.command;

import com.vetsoftware.app.cashregister.domain.CashPaymentMethod;
import java.math.BigDecimal;

/** Línea de pago que la orquestación (POS / cuenta abierta) envía a caja: medio + monto (positivo). */
public record CashPaymentLine(CashPaymentMethod method, BigDecimal amount) {}
