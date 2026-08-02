package com.vetsoftware.app.cashregister.application.port.out;

import java.math.BigDecimal;
import java.util.List;

/** Telemetría agregada de aperturas, cierres y diferencias de caja. */
public interface CashMetrics {

  void opened();

  void closed(List<BigDecimal> differences);
}
