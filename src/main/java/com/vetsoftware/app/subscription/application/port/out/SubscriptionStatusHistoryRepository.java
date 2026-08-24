package com.vetsoftware.app.subscription.application.port.out;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscription.domain.SubscriptionStatusChange;

/**
 * Puerto de salida de la bitacora de estados. <strong>Solo anade</strong>: el
 * nombre del metodo lo dice para que nadie busque un {@code save} que
 * sobreescriba. Una bitacora que se puede reescribir no prueba nada.
 */
public interface SubscriptionStatusHistoryRepository {

    SubscriptionStatusChange append(SubscriptionStatusChange change);

    PageResult<SubscriptionStatusChange> findAllBySubscriptionIdAndCompanyId(Long subscriptionId,
            Long companyId, int page, int pageSize);
}
