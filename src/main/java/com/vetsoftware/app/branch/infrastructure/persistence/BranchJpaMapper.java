package com.vetsoftware.app.branch.infrastructure.persistence;

import com.vetsoftware.app.branch.domain.Branch;
import com.vetsoftware.app.branch.domain.CityRef;
import com.vetsoftware.app.branch.domain.CompanyRef;
import com.vetsoftware.app.city.infrastructure.persistence.CityJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class BranchJpaMapper {

  public BranchJpaEntity toJpa(Branch branch, CityJpaEntity city, CompanyJpaEntity company) {
    BranchJpaEntity entity = new BranchJpaEntity();
    entity.setId(branch.getId());
    entity.setName(branch.getName());
    entity.setCode(branch.getCode());
    entity.setAddress(branch.getAddress());
    entity.setPhone(branch.getPhone());
    entity.setCity(city);
    entity.setCompany(company);
    entity.setCreatedDate(branch.getCreatedDate());
    entity.setActive(branch.isActive());
    return entity;
  }

  public Branch toDomain(BranchJpaEntity entity) {
    CityJpaEntity c = entity.getCity();
    CompanyJpaEntity co = entity.getCompany();
    return toDomain(
        entity,
        new CityRef(c.getId(), c.getName()),
        new CompanyRef(co.getId(), co.getName(), co.getIdentifier()));
  }

  public Branch toDomain(BranchJpaEntity entity, CityRef cityRef, CompanyRef companyRef) {
    return new Branch(
        entity.getId(),
        entity.getName(),
        entity.getCode(),
        entity.getAddress(),
        entity.getPhone(),
        cityRef,
        companyRef,
        entity.getCreatedDate(),
        entity.isActive());
  }
}
