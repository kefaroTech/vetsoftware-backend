package com.vetsoftware.app.company.infrastructure.persistence;

import com.vetsoftware.app.company.application.port.out.ConsultationChildrenQueryPort;
import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaConsultationChildrenQueryPort implements ConsultationChildrenQueryPort {
  private final ConsultationJpaRepository jpaRepository;

  public JpaConsultationChildrenQueryPort(ConsultationJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public boolean existsActiveByCompanyId(Long parentId) {
    return jpaRepository.existsByCompany_Id(parentId);
  }
}
