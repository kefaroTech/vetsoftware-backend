package com.vetsoftware.app.salesreport.application.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Conciliación (F6): documentos emitidos vs. validados por la DIAN. Conteos por estado + el detalle de
 * los que requieren atención (rechazados, contingencias y pendientes de validación).
 */
public record ReconciliationDto(
        LocalDate dateFrom,
        LocalDate dateTo,
        long total,
        long validados,
        long rechazados,
        long contingencia,
        long pendientes,
        List<PendingDto> needsAttention
) {
    public record PendingDto(Long id, String documentType, String prefix, Long consecutive,
                             LocalDate issueDate, String dianStatus, String cufe, String cude) {}
}
