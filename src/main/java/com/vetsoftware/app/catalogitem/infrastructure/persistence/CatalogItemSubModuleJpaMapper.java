package com.vetsoftware.app.catalogitem.infrastructure.persistence;

import com.vetsoftware.app.catalogitem.domain.CatalogItemSubModule;
import com.vetsoftware.app.catalogitem.domain.SubModuleRef;
import com.vetsoftware.app.submodule.infrastructure.persistence.SubModuleJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class CatalogItemSubModuleJpaMapper {

    public CatalogItemSubModuleJpaEntity toJpa(CatalogItemSubModule link,
            CatalogItemJpaEntity catalogItem, SubModuleJpaEntity subModule) {
        CatalogItemSubModuleJpaEntity entity = new CatalogItemSubModuleJpaEntity();
        entity.setId(link.getId());
        entity.setCatalogItem(catalogItem);
        entity.setSubModule(subModule);
        entity.setCreatedDate(link.getCreatedDate());
        entity.setEnabled(link.isEnabled());
        return entity;
    }

    /**
     * Camino de lectura: el {@code @EntityGraph} del repositorio ya hidrató el
     * submódulo, así que leer su nombre y su código no dispara ninguna consulta.
     */
    public CatalogItemSubModule toDomain(CatalogItemSubModuleJpaEntity entity) {
        SubModuleJpaEntity subModule = entity.getSubModule();
        return toDomain(entity,
                new SubModuleRef(subModule.getId(), subModule.getName(), subModule.getCode()));
    }

    /**
     * Camino de escritura: reutiliza el {@code SubModuleRef} que el caso de uso ya
     * resolvió por el puerto. Leer {@code entity.getSubModule().getName()} después
     * de un {@code getReferenceById} inicializaría el proxy y añadiría un SELECT
     * por cada alta.
     */
    public CatalogItemSubModule toDomain(CatalogItemSubModuleJpaEntity entity, SubModuleRef ref) {
        return new CatalogItemSubModule(entity.getId(), entity.getCatalogItem().getId(), ref,
                entity.getCreatedDate(), entity.isEnabled());
    }
}
