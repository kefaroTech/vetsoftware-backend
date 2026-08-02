package com.vetsoftware.app.company.infrastructure.persistence;

import com.vetsoftware.app.company.application.port.out.SurgeryChildrenQueryPort;
import com.vetsoftware.app.surgery.infrastructure.persistence.SurgeryJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaSurgeryChildrenQueryPort implements SurgeryChildrenQueryPort {
  private final SurgeryJpaRepository jpaRepository;

  public JpaSurgeryChildrenQueryPort(SurgeryJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public boolean existsActiveByCompanyId(Long parentId) {
    return jpaRepository.existsByCompany_Id(parentId);
  }
}
