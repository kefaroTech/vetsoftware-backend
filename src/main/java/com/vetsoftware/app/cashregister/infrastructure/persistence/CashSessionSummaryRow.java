package com.vetsoftware.app.cashregister.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Proyección liviana del historial de caja con los nombres de los responsables de apertura y cierre. */
public interface CashSessionSummaryRow {
    Long getId();
    Long getBranchId();
    String getBranchName();
    String getTerminal();
    String getStatus();
    Long getOpenedByEmployeeId();
    String getOpenedByEmployeeName();
    LocalDateTime getOpenedAt();
    BigDecimal getOpeningFloat();
    BigDecimal getClosingTotal();
    Long getClosedByEmployeeId();
    String getClosedByEmployeeName();
    LocalDateTime getClosedAt();
    String getNote();
    Long getVersion();
}
