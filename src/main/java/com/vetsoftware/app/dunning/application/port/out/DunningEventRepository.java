package com.vetsoftware.app.dunning.application.port.out;

import com.vetsoftware.app.dunning.domain.DunningEvent;
import com.vetsoftware.app.shared.pagination.PageResult;
import java.util.Optional;

/**
 * Sin {@code delete} y sin {@code findById} ancho.
 *
 * <p>
 * Sin {@code delete} porque la tabla es append-only y su valor esta en que no
 * se pueda reescribir. Sin variante ancha porque
 * {@code CARGA_POR_ID_ACOTADA_POR_EMPRESA} (BE-COV) marca al caso de uso que
 * conoce la ancha y no la acotada, y la forma de no poder equivocarse es que la
 * ancha no exista.
 */
public interface DunningEventRepository {

    DunningEvent save(DunningEvent event);

    Optional<DunningEvent> findByIdAndCompanyId(Long id, Long companyId);

    PageResult<DunningEvent> findAllBySubscriptionIdAndCompanyId(Long subscriptionId,
            Long companyId, int page, int pageSize);

    PageResult<DunningEvent> findAllByCompanyId(Long companyId, int page, int pageSize);

    /** Barrido de plataforma cross-tenant. Solo lo consume un puerto SYSTEM. */
    PageResult<DunningEvent> findAll(int page, int pageSize);
}
