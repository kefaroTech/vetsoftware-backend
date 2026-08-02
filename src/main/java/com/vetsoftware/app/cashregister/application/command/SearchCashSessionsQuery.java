package com.vetsoftware.app.cashregister.application.command;

import java.time.LocalDate;

/**
 * Historial paginado de sesiones de caja con filtros opcionales, más reciente
 * primero.
 */
public record SearchCashSessionsQuery(Long companyId, Long branchId, Long employeeId,
        LocalDate from, LocalDate to, int page, int pageSize) {
}
