package com.vetsoftware.app.subscription.infrastructure.persistence;

import com.vetsoftware.app.catalogitem.infrastructure.persistence.CatalogItemJpaRepository;
import com.vetsoftware.app.subscription.application.port.out.CatalogItemValidationPort;
import org.springframework.stereotype.Component;

/**
 * Unico archivo de este slice que conoce el catalogo, y solo para preguntar si
 * el articulo existe: sus datos se congelan en la linea al firmar, no se
 * releen.
 */
@Component("subscriptionJpaCatalogItemValidationPort")
public class JpaCatalogItemValidationPort implements CatalogItemValidationPort {

    private final CatalogItemJpaRepository catalogItemJpaRepository;

    public JpaCatalogItemValidationPort(CatalogItemJpaRepository catalogItemJpaRepository) {
        this.catalogItemJpaRepository = catalogItemJpaRepository;
    }

    @Override
    public void validateExists(Long catalogItemId) {
        if (catalogItemId == null || !catalogItemJpaRepository.existsById(catalogItemId)) {
            throw new IllegalArgumentException("Catalog item not found: " + catalogItemId);
        }
    }
}
