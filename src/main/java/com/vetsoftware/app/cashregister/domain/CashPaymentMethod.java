package com.vetsoftware.app.cashregister.domain;

/**
 * Medio de pago de un movimiento de caja. Enum propio de la feature: los enums externos ({@code
 * electronicdocument.PaymentMeans}, {@code debtopenaccount.PaymentMethod}) se mapean en los
 * adapters de orquestación (respeta el vertical slicing). Solo {@link #CASH} cuenta para el conteo
 * físico y la diferencia del arqueo; {@link #CARD}/{@link #TRANSFER} se registran para conciliar
 * por medio de pago.
 */
public enum CashPaymentMethod {
  CASH,
  CARD,
  TRANSFER
}
