package com.vetsoftware.app.catalogitem.infrastructure.persistence;

import com.vetsoftware.app.catalogitem.domain.CatalogItem;
import org.springframework.stereotype.Component;

@Component
public class CatalogItemJpaMapper {

    public CatalogItemJpaEntity toJpa(CatalogItem item) {
        CatalogItemJpaEntity entity = new CatalogItemJpaEntity();
        entity.setId(item.getId());
        entity.setCode(item.getCode());
        entity.setName(item.getName());
        entity.setShortDescription(item.getShortDescription());
        entity.setLongDescription(item.getLongDescription());
        entity.setItemType(item.getItemType());
        entity.setCapacityUnit(item.getCapacityUnit());
        entity.setCore(item.isCore());
        entity.setMinQuantity(item.getMinQuantity());
        entity.setMaxQuantity(item.getMaxQuantity());
        entity.setSortOrder(item.getSortOrder());
        entity.setStatus(item.getStatus());
        // Se copia de vuelta porque toJpa construye una entidad NUEVA en cada save:
        // sin esta linea, cada actualizacion de un articulo borraba en silencio los
        // dias de prueba que la capa I habia sembrado. El campo es de solo lectura en
        // el agregado -update() no lo toca-, asi que esto conserva, no edita.
        entity.setDefaultTrialDays(item.getDefaultTrialDays());
        entity.setCreatedDate(item.getCreatedDate());
        entity.setVersion(item.getVersion());
        entity.setEnabled(item.isEnabled());
        return entity;
    }

    public CatalogItem toDomain(CatalogItemJpaEntity entity) {
        return new CatalogItem(entity.getId(), entity.getCode(), entity.getName(),
                entity.getShortDescription(), entity.getLongDescription(), entity.getItemType(),
                entity.getCapacityUnit(), entity.isCore(), entity.getMinQuantity(),
                entity.getMaxQuantity(), entity.getSortOrder(), entity.getStatus(),
                entity.getDefaultTrialDays(), entity.getCreatedDate(), entity.getVersion(),
                entity.isEnabled());
    }
}
