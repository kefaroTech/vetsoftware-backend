package com.vetsoftware.app.company.infrastructure.persistence;

import com.vetsoftware.app.company.application.port.out.DayCareChildrenQueryPort;
import com.vetsoftware.app.daycare.infrastructure.persistence.DayCareJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaDayCareChildrenQueryPort implements DayCareChildrenQueryPort {
  private final DayCareJpaRepository jpaRepository;

  public JpaDayCareChildrenQueryPort(DayCareJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public boolean existsActiveByCompanyId(Long parentId) {
    return jpaRepository.existsByCompany_Id(parentId);
  }
}
