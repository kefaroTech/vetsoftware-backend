package com.vetsoftware.app.clinicalhistory.application.query;

import com.vetsoftware.app.clinicalhistory.domain.ClinicalEventType;
import java.time.LocalDate;
import java.util.List;

/**
 * Criterios de la historia clinica de un animal.
 *
 * <p>
 * {@code q} se anade en BE-06: es el buscador de la pantalla, que filtraba en
 * cliente sobre la historia completa. Paginada, ese filtro solo veria la pagina
 * ya cargada.
 */
public record GetClinicalHistoryQuery(Long animalId, Long companyId, List<ClinicalEventType> types,
        LocalDate from, LocalDate to, String q) {
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
