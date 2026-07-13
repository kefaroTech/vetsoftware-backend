package com.vetsoftware.app.inventory.application.command;

import java.time.LocalDate;

/** Libro de compras paginado (movimientos de entrada), con sede y rango de fechas opcionales. */
public record SearchPurchasesQuery(Long companyId, Long branchId, LocalDate from, LocalDate to,
                                   int page, int pageSize) {}
