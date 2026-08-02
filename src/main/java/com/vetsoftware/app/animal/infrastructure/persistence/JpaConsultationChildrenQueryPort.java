package com.vetsoftware.app.animal.infrastructure.persistence;

import com.vetsoftware.app.animal.application.port.out.ConsultationChildrenQueryPort;
import com.vetsoftware.app.consultation.infrastructure.persistence.ConsultationJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class JpaConsultationChildrenQueryPort implements ConsultationChildrenQueryPort {
  private final ConsultationJpaRepository jpaRepository;

  public JpaConsultationChildrenQueryPort(ConsultationJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public boolean existsActiveByAnimalId(Long parentId) {
    return jpaRepository.existsByAnimal_Id(parentId);
  }
}
