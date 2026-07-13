package com.vetsoftware.app.cashregister.application.command;

import java.time.LocalDate;

/** Historial paginado de sesiones de caja por (empresa, sede) con rango de fechas opcional, más reciente primero. */
public record SearchCashSessionsQuery(Long companyId, Long branchId, LocalDate from, LocalDate to,
                                      int page, int pageSize) {}
