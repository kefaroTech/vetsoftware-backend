package com.vetsoftware.app.configurator.application.port.in;

import com.vetsoftware.app.configurator.application.dto.ConfiguratorQuestionDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Listado sin filtro de empresa, así que {@code hasRole('SYSTEM')} a secas —
 * `LISTADOS_SIN_EMPRESA_SOLO_SYSTEM`, regla dura. Aquí no es que falte el
 * filtro: la tabla no tiene {@code company_id} y el cuestionario es el mismo
 * para todo el mundo.
 */
public interface ListConfiguratorQuestionsUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<ConfiguratorQuestionDto> listAll(int page, int pageSize);
}
