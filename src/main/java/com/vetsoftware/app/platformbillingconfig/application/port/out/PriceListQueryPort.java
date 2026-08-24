package com.vetsoftware.app.platformbillingconfig.application.port.out;

import com.vetsoftware.app.platformbillingconfig.domain.PriceListRef;
import java.util.Optional;

/**
 * Resuelve la tarifa por defecto contra la feature {@code pricelist}.
 *
 * <p>
 * No tiene variante acotada por empresa —{@code findByIdAndCompanyId}— y no
 * debe tenerla: {@code price_lists} es una tabla global de plataforma, sin
 * {@code company_id}. Las reglas BE-COV que exigen la variante acotada solo
 * miran referencias a entidades que pertenecen a una empresa.
 */
public interface PriceListQueryPort {
    Optional<PriceListRef> findById(Long priceListId);
}
