package com.vetsoftware.app.subscriptionitemlimit.application.port.out;

import com.vetsoftware.app.subscriptionitemlimit.domain.SubscriptionItemLimit;
import java.util.List;
import java.util.Optional;

/**
 * Adaptador de salida de los techos congelados.
 *
 * <p>
 * Todas las lecturas de una fila concreta llevan la empresa: el techo de una
 * clínica no se carga por un id suelto que escribe el cliente. La única
 * consulta sin empresa es la de propagación, que por definición cruza tenants y
 * cuyo caso de uso está cerrado a un principal cross-tenant.
 */
public interface SubscriptionItemLimitRepository {

    SubscriptionItemLimit save(SubscriptionItemLimit limit);

    List<SubscriptionItemLimit> saveAll(List<SubscriptionItemLimit> limits);

    Optional<SubscriptionItemLimit> findByCompanyIdAndSubscriptionItemIdAndLimitDimensionId(
            Long companyId, Long subscriptionItemId, Long limitDimensionId);

    List<SubscriptionItemLimit> findAllByCompanyId(Long companyId);

    /**
     * Los techos congelados de las líneas <em>vivas</em> de un artículo sobre un
     * eje, en todas las empresas. Es la consulta de la propagación de mejoras
     * (D-75).
     */
    List<SubscriptionItemLimit> findAllLiveByCatalogItemIdAndLimitDimensionId(Long catalogItemId,
            Long limitDimensionId);
}
