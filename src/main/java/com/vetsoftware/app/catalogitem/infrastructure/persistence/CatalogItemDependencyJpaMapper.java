package com.vetsoftware.app.catalogitem.infrastructure.persistence;

import com.vetsoftware.app.catalogitem.domain.CatalogItemDependency;
import org.springframework.stereotype.Component;

@Component
public class CatalogItemDependencyJpaMapper {

    public CatalogItemDependencyJpaEntity toJpa(CatalogItemDependency dependency,
            CatalogItemJpaEntity catalogItem, CatalogItemJpaEntity relatedItem) {
        CatalogItemDependencyJpaEntity entity = new CatalogItemDependencyJpaEntity();
        entity.setId(dependency.getId());
        entity.setCatalogItem(catalogItem);
        entity.setRelatedItem(relatedItem);
        entity.setRelationType(dependency.getRelationType());
        entity.setNote(dependency.getNote());
        entity.setCreatedDate(dependency.getCreatedDate());
        entity.setEnabled(dependency.isEnabled());
        return entity;
    }

    /**
     * Solo se leen los identificadores de las dos asociaciones. Un proxy LAZY los
     * sirve sin ir a la base —el valor está en la columna de la clave foránea—, así
     * que este camino no necesita {@code @EntityGraph} ni produce N+1.
     */
    public CatalogItemDependency toDomain(CatalogItemDependencyJpaEntity entity) {
        return new CatalogItemDependency(entity.getId(), entity.getCatalogItem().getId(),
                entity.getRelatedItem().getId(), entity.getRelationType(), entity.getNote(),
                entity.getCreatedDate(), entity.isEnabled());
    }
}
