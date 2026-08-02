package com.vetsoftware.app.withholdingconfig.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.withholdingconfig.domain.CompanyRef;
import com.vetsoftware.app.withholdingconfig.domain.WithholdingConfig;
import org.springframework.stereotype.Component;

@Component
public class WithholdingConfigJpaMapper {

  public WithholdingConfigJpaEntity toJpa(WithholdingConfig config, CompanyJpaEntity company) {
    WithholdingConfigJpaEntity entity = new WithholdingConfigJpaEntity();
    entity.setId(config.getId());
    entity.setCompany(company);
    entity.setReteFuenteRate(config.getReteFuenteRate());
    entity.setReteIvaRate(config.getReteIvaRate());
    entity.setReteIcaRate(config.getReteIcaRate());
    entity.setCreatedDate(config.getCreatedDate());
    entity.setEnabled(config.isEnabled());
    return entity;
  }

  public WithholdingConfig toDomain(WithholdingConfigJpaEntity entity) {
    CompanyJpaEntity c = entity.getCompany();
    CompanyRef ref = c == null ? null : new CompanyRef(c.getId(), c.getName(), c.getIdentifier());
    return toDomain(entity, ref);
  }

  public WithholdingConfig toDomain(WithholdingConfigJpaEntity entity, CompanyRef ref) {
    return new WithholdingConfig(
        entity.getId(),
        ref,
        entity.getReteFuenteRate(),
        entity.getReteIvaRate(),
        entity.getReteIcaRate(),
        entity.getCreatedDate(),
        entity.isEnabled());
  }
}
