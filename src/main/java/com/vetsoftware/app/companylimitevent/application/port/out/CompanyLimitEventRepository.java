package com.vetsoftware.app.companylimitevent.application.port.out;

import com.vetsoftware.app.companylimitevent.domain.CompanyLimitEvent;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Adaptador de salida de la bitácora de cupo.
 *
 * <p>
 * <strong>Solo se agrega.</strong> No declara ni actualización ni borrado, y
 * esa ausencia es la garantía: una prueba que se puede reescribir no prueba
 * nada.
 */
public interface CompanyLimitEventRepository {

    CompanyLimitEvent append(CompanyLimitEvent event);

    /** Los hechos de una empresa en un rango. Acotado siempre por empresa. */
    List<CompanyLimitEvent> findAllByCompanyIdBetween(Long companyId, LocalDateTime from,
            LocalDateTime to);
}
