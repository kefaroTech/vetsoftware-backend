package com.vetsoftware.app.configurator.infrastructure.persistence;

import com.vetsoftware.app.catalogitem.infrastructure.persistence.CatalogItemJpaRepository;
import com.vetsoftware.app.configurator.application.port.out.CatalogItemValidationPort;
import org.springframework.stereotype.Component;

/**
 * El único archivo del slice que conoce a {@code catalogitem}, y el cruce está
 * acotado a lo que permite el vertical slicing:
 * {@code infrastructure/persistence} importando el {@code XxxJpaRepository} de
 * la otra feature, nunca su dominio ni sus DTO.
 *
 * <p>
 * Se apoya solo en {@code existsById}, heredado de {@code JpaRepository}. No
 * lee ni un getter de {@code CatalogItemJpaEntity} a propósito: el configurador
 * no necesita ningún campo del artículo, y depender de la forma de una entidad
 * ajena es cómo un cambio inocente en otra feature rompe esta.
 *
 * <p>
 * {@code catalog_items} lleva {@code @SQLRestriction("enabled = true")}, así
 * que un artículo retirado del catálogo cuenta como inexistente — que es justo
 * lo que se quiere al colgarle un efecto nuevo.
 */
@Component
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
