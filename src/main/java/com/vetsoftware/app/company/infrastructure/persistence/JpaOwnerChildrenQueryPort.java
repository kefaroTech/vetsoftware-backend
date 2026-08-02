package com.vetsoftware.app.company.infrastructure.persistence;

import com.vetsoftware.app.company.application.port.out.OwnerChildrenQueryPort;
import com.vetsoftware.app.owner.infrastructure.persistence.OwnerJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaOwnerChildrenQueryPort implements OwnerChildrenQueryPort {
  private final OwnerJpaRepository jpaRepository;

  public JpaOwnerChildrenQueryPort(OwnerJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public boolean existsActiveByCompanyId(Long parentId) {
    return jpaRepository.existsByCompany_Id(parentId);
  }
}
