package com.vetsoftware.app.quote.application.port.out;

import com.vetsoftware.app.quote.domain.CompanyRef;
import java.util.Optional;

/**
 * Resuelve la empresa destinataria. No declara variante acotada porque la
 * entidad referida ES la empresa: acotarla por si misma no significa nada.
 */
public interface CompanyQueryPort {
    Optional<CompanyRef> findById(Long companyId);
}
