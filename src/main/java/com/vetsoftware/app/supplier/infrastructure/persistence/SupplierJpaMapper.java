package com.vetsoftware.app.supplier.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.supplier.domain.CompanyRef;
import com.vetsoftware.app.supplier.domain.Supplier;
import org.springframework.stereotype.Component;

@Component
public class SupplierJpaMapper {

  public SupplierJpaEntity toJpa(Supplier supplier, CompanyJpaEntity company) {
    SupplierJpaEntity entity = new SupplierJpaEntity();
    entity.setId(supplier.getId());
    entity.setName(supplier.getName());
    entity.setTaxId(supplier.getTaxId());
    entity.setContactName(supplier.getContactName());
    entity.setPhone(supplier.getPhone());
    entity.setEmail(supplier.getEmail());
    entity.setAddress(supplier.getAddress());
    entity.setPaymentTermsDays(supplier.getPaymentTermsDays());
    entity.setNotes(supplier.getNotes());
    entity.setCompany(company);
    entity.setCreatedDate(supplier.getCreatedDate());
    entity.setUpdatedDate(supplier.getUpdatedDate());
    entity.setUpdatedBy(supplier.getUpdatedBy());
    entity.setVersion(supplier.getVersion());
    entity.setEnabled(supplier.isEnabled());
    return entity;
  }

  public Supplier toDomain(SupplierJpaEntity entity) {
    CompanyJpaEntity c = entity.getCompany();
    return toDomain(entity, new CompanyRef(c.getId(), c.getName(), c.getIdentifier()));
  }

  public Supplier toDomain(SupplierJpaEntity entity, CompanyRef companyRef) {
    return new Supplier(
        entity.getId(),
        entity.getName(),
        entity.getTaxId(),
        entity.getContactName(),
        entity.getPhone(),
        entity.getEmail(),
        entity.getAddress(),
        entity.getPaymentTermsDays(),
        entity.getNotes(),
        companyRef,
        entity.getCreatedDate(),
        entity.getUpdatedDate(),
        entity.getUpdatedBy(),
        entity.getVersion(),
        entity.isEnabled());
  }
}
