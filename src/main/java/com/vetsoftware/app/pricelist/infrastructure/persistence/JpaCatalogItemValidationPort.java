package com.vetsoftware.app.pricelist.infrastructure.persistence;

import com.vetsoftware.app.catalogitem.infrastructure.persistence.CatalogItemJpaRepository;
import com.vetsoftware.app.pricelist.application.port.out.CatalogItemValidationPort;
import org.springframework.stereotype.Component;

/**
 * El unico archivo de esta feature que conoce el slice {@code catalogitem}, y
 * es la excepcion acotada que {@code CLAUDE.md} permite:
 * {@code <feature>/infrastructure/persistence} puede importar el
 * {@code XxxJpaRepository} de otra feature.
 *
 * <p>
 * Se apoya en {@code existsById}, que es de {@code JpaRepository} y por tanto
 * no depende de ningun metodo derivado ni de ningun getter de la entidad ajena.
 * El {@code @SQLRestriction} de {@code CatalogItemJpaEntity} -si lo lleva, y su
 * ficha dice que la tabla tiene {@code enabled}- hace que un articulo dado de
 * baja no exista para esta comprobacion, que es lo correcto: no se le pone
 * precio a lo que ya no se vende.
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
