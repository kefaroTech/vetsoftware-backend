package com.vetsoftware.app.subscription.application.port.in;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscription.application.dto.SubscriptionItemDto;
import java.time.LocalDate;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Lo contratado. Con {@code onDate} responde la pregunta que da sentido a la
 * tabla —que tenia esta clinica el 3 de marzo— aplicando el criterio de
 * {@code EffectivePeriod}; con {@code onDate} nulo devuelve el expediente
 * completo, incluidas las lineas ya cerradas, que <strong>siguen ahi</strong>.
 */
public interface ListSubscriptionItemsUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('subscription.read') and"
            + " @authz.isMyCompany(#companyId))")
    PageResult<SubscriptionItemDto> listAll(Long subscriptionId, Long companyId, LocalDate onDate,
            int page, int pageSize);
}
