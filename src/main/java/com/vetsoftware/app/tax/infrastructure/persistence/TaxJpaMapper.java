package com.vetsoftware.app.tax.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.tax.domain.CompanyRef;
import com.vetsoftware.app.tax.domain.Tax;
import org.springframework.stereotype.Component;

@Component
public class TaxJpaMapper {
  public TaxJpaEntity toJpa(Tax tax, CompanyJpaEntity company) {
    TaxJpaEntity entity = new TaxJpaEntity();
    entity.setId(tax.getId());
    entity.setName(tax.getName());
    entity.setPercentage(tax.getPercentage());
    entity.setTaxScheme(tax.getTaxScheme());
    entity.setCompany(company);
    entity.setCreatedDate(tax.getCreatedDate());
    entity.setUpdatedDate(tax.getUpdatedDate());
    entity.setUpdatedBy(tax.getUpdatedBy());
    entity.setVersion(tax.getVersion());
    entity.setEnabled(tax.isEnabled());
    return entity;
  }

  public Tax toDomain(TaxJpaEntity entity) {
    CompanyJpaEntity c = entity.getCompany();
    return toDomain(
        entity, c == null ? null : new CompanyRef(c.getId(), c.getName(), c.getIdentifier()));
  }

  public Tax toDomain(TaxJpaEntity entity, CompanyRef companyRef) {
    return new Tax(
        entity.getId(),
        entity.getName(),
        entity.getPercentage(),
        entity.getTaxScheme(),
        companyRef,
        entity.getCreatedDate(),
        entity.getUpdatedDate(),
        entity.getUpdatedBy(),
        entity.getVersion(),
        entity.isEnabled());
  }
}
