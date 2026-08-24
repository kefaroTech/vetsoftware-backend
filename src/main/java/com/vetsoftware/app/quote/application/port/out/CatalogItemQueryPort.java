package com.vetsoftware.app.quote.application.port.out;

import com.vetsoftware.app.quote.domain.CatalogItemRef;
import java.util.Optional;

/**
 * Lee el articulo del catalogo UNA SOLA VEZ, al congelar la linea. Catalogo
 * global de plataforma, sin tenant.
 */
public interface CatalogItemQueryPort {
    Optional<CatalogItemRef> findActiveById(Long catalogItemId);
}
