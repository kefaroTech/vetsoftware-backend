package com.vetsoftware.app.quote.application.port.in;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Barrido de plataforma que marca EXPIRED lo que ya vencio.
 *
 * <p>
 * Cruza todos los tenants por definicion -y por eso ix_quotes_expiring no
 * empieza por company_id-, asi que va cerrado a hasRole('SYSTEM') a secas.
 */
public interface ExpireOverdueQuotesUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    int expireOverdue(int batchSize);
}
