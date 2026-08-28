package com.vetsoftware.app.accountmapping.infrastructure.persistence;

import com.vetsoftware.app.accountmapping.application.port.out.CatalogItemValidationPort;
import com.vetsoftware.app.catalogitem.infrastructure.persistence.CatalogItemJpaRepository;
import org.springframework.stereotype.Component;

/**
 * El unico archivo de este slice que conoce a {@code catalogitem}.
 *
 * <p>
 * Usa el {@code existsById} heredado de {@code JpaRepository} y no lee ningun
 * campo del articulo: el mapeo solo necesita saber que existe, porque
 * {@code fk_account_mappings_item} es {@code RESTRICT} y un id inventado
 * saldria como error de integridad en vez de como «ese articulo no existe».
 *
 * <p>
 * El nombre de bean va cualificado porque el vertical slicing repite nombres de
 * clase entre features.
 */
@Component("accountMappingJpaCatalogItemValidationPort")
public class JpaCatalogItemValidationPort implements CatalogItemValidationPort {

    private final CatalogItemJpaRepository catalogItemJpaRepository;

    public JpaCatalogItemValidationPort(CatalogItemJpaRepository catalogItemJpaRepository) {
        this.catalogItemJpaRepository = catalogItemJpaRepository;
    }

    @Override
    public boolean existsById(Long catalogItemId) {
        return catalogItemId != null && catalogItemJpaRepository.existsById(catalogItemId);
    }
}
