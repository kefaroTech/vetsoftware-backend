package com.vetsoftware.app.inventory.application.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Renglón del reporte de kardex (ya con etiquetas ES y saldo corrido). */
public record KardexReportLine(
    LocalDateTime createdDate,
    String typeLabel,
    String referenceLabel,
    Long lotId,
    int quantity,
    BigDecimal unitCost,
    int runningBalance) {}
