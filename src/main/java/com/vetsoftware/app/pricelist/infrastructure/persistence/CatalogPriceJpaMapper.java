package com.vetsoftware.app.pricelist.infrastructure.persistence;

import com.vetsoftware.app.pricelist.domain.CatalogPrice;
import org.springframework.stereotype.Component;

@Component
public class CatalogPriceJpaMapper {

    public CatalogPriceJpaEntity toJpa(CatalogPrice price) {
        CatalogPriceJpaEntity entity = new CatalogPriceJpaEntity();
        entity.setId(price.getId());
        entity.setPriceListId(price.getPriceListId());
        entity.setCatalogItemId(price.getCatalogItemId());
        entity.setBillingCycle(price.getBillingCycle());
        entity.setTierMin(price.getTierMin());
        entity.setTierMax(price.getTierMax());
        entity.setIncludedQuantity(price.getIncludedQuantity());
        entity.setUnitAmount(price.getUnitAmount());
        entity.setSetupAmount(price.getSetupAmount());
        entity.setTaxRate(price.getTaxRate());
        entity.setTaxTreatment(price.getTaxTreatment());
        entity.setCreatedDate(price.getCreatedDate());
        entity.setVersion(price.getVersion());
        entity.setEnabled(price.isEnabled());
        return entity;
    }

    public CatalogPrice toDomain(CatalogPriceJpaEntity entity) {
        return new CatalogPrice(entity.getId(), entity.getPriceListId(), entity.getCatalogItemId(),
                entity.getBillingCycle(), entity.getTierMin(), entity.getTierMax(),
                entity.getIncludedQuantity(), entity.getUnitAmount(), entity.getSetupAmount(),
                entity.getTaxRate(), entity.getTaxTreatment(), entity.getCreatedDate(),
                entity.getVersion(), entity.isEnabled());
    }
}
