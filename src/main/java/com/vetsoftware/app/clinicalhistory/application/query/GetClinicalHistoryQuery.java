package com.vetsoftware.app.clinicalhistory.application.query;

import com.vetsoftware.app.clinicalhistory.domain.ClinicalEventType;
import java.time.LocalDate;
import java.util.List;

/**
 * Criterios de la historia clinica de un animal.
 *
 * <p>
 * {@code q} y {@code consultationId} se anaden en BE-06. {@code q} es el
 * buscador de la pantalla, que filtraba en cliente sobre la historia completa.
 * {@code consultationId} acota a los procedimientos derivados de una consulta:
 * el detalle los sacaba del array cargado, y paginado pueden estar en otra
 * pagina.
 */
public record GetClinicalHistoryQuery(Long animalId, Long companyId, List<ClinicalEventType> types,
        LocalDate from, LocalDate to, String q, Long consultationId) {
    public GetClinicalHistoryQuery {
        if (animalId == null)
            throw new IllegalArgumentException("animalId is required");
        if (companyId == null)
            throw new IllegalArgumentException("companyId is required");
        if (types == null)
            types = List.of();
        q = q == null || q.isBlank() ? null : q.trim();
        if (from != null && to != null && to.isBefore(from)) {
            throw new IllegalArgumentException("'to' cannot be before 'from'");
        }
    }
}
