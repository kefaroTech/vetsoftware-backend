package com.vetsoftware.app.pricelist.infrastructure.persistence;

import com.vetsoftware.app.pricelist.domain.PriceList;
import org.springframework.stereotype.Component;

@Component
public class PriceListJpaMapper {

    public PriceListJpaEntity toJpa(PriceList priceList) {
        PriceListJpaEntity entity = new PriceListJpaEntity();
        entity.setId(priceList.getId());
        entity.setCode(priceList.getCode());
        entity.setName(priceList.getName());
        entity.setCurrency(priceList.getCurrency());
        entity.setValidFrom(priceList.getValidFrom());
        entity.setValidTo(priceList.getValidTo());
        entity.setStatus(priceList.getStatus());
        entity.setPublishedAt(priceList.getPublishedAt());
        entity.setPublishedBySystemUserId(priceList.getPublishedBySystemUserId());
        entity.setCreatedDate(priceList.getCreatedDate());
        entity.setVersion(priceList.getVersion());
        entity.setEnabled(priceList.isEnabled());
        return entity;
    }

    public PriceList toDomain(PriceListJpaEntity entity) {
        return new PriceList(entity.getId(), entity.getCode(), entity.getName(),
                entity.getCurrency(), entity.getValidFrom(), entity.getValidTo(),
                entity.getStatus(), entity.getPublishedAt(), entity.getPublishedBySystemUserId(),
                entity.getCreatedDate(), entity.getVersion(), entity.isEnabled());
    }
}
