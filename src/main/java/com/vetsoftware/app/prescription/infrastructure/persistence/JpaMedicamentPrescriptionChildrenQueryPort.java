package com.vetsoftware.app.prescription.infrastructure.persistence;

import com.vetsoftware.app.medicamentprescription.infrastructure.persistence.MedicamentPrescriptionJpaRepository;
import com.vetsoftware.app.prescription.application.port.out.MedicamentPrescriptionChildrenQueryPort;
import org.springframework.stereotype.Component;

@Component
public class JpaMedicamentPrescriptionChildrenQueryPort
    implements MedicamentPrescriptionChildrenQueryPort {
  private final MedicamentPrescriptionJpaRepository jpaRepository;

  public JpaMedicamentPrescriptionChildrenQueryPort(
      MedicamentPrescriptionJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public boolean existsActiveByPrescriptionId(Long parentId) {
    return jpaRepository.existsByPrescription_Id(parentId);
  }
}
