package com.vetsoftware.app.catalogitem.infrastructure.persistence;

import com.vetsoftware.app.catalogitem.domain.BundleComponent;
import org.springframework.stereotype.Component;

@Component
public class BundleComponentJpaMapper {

    public BundleComponentJpaEntity toJpa(BundleComponent component,
            CatalogItemJpaEntity bundleItem, CatalogItemJpaEntity componentItem) {
        BundleComponentJpaEntity entity = new BundleComponentJpaEntity();
        entity.setId(component.getId());
        entity.setBundleItem(bundleItem);
        entity.setComponentItem(componentItem);
        entity.setQuantity(component.getQuantity());
        entity.setCreatedDate(component.getCreatedDate());
        entity.setEnabled(component.isEnabled());
        return entity;
    }

    /** Mismo criterio que en las dependencias: solo se leen los identificadores. */
    public BundleComponent toDomain(BundleComponentJpaEntity entity) {
        return new BundleComponent(entity.getId(), entity.getBundleItem().getId(),
                entity.getComponentItem().getId(), entity.getQuantity(), entity.getCreatedDate(),
                entity.isEnabled());
    }
}
