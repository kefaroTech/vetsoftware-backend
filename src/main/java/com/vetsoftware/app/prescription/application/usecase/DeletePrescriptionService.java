package com.vetsoftware.app.prescription.application.usecase;

import com.vetsoftware.app.prescription.application.port.in.DeletePrescriptionUseCase;
import com.vetsoftware.app.prescription.application.port.out.MedicamentPrescriptionChildrenQueryPort;
import com.vetsoftware.app.prescription.application.port.out.PrescriptionRepository;
import com.vetsoftware.app.prescription.domain.PrescriptionHasActiveChildrenException;
import com.vetsoftware.app.prescription.domain.PrescriptionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "prescription.delete")
@Service
public class DeletePrescriptionService implements DeletePrescriptionUseCase {
  private final PrescriptionRepository repository;
  private final MedicamentPrescriptionChildrenQueryPort medicamentPrescriptionChildrenQueryPort;

  public DeletePrescriptionService(
      PrescriptionRepository repository,
      MedicamentPrescriptionChildrenQueryPort medicamentPrescriptionChildrenQueryPort) {
    this.repository = repository;
    this.medicamentPrescriptionChildrenQueryPort = medicamentPrescriptionChildrenQueryPort;
  }

  @Override
  @Transactional
  public void execute(Long id, Long companyId) {
    (companyId == null ? repository.findById(id) : repository.findByIdAndCompanyId(id, companyId))
        .orElseThrow(() -> new PrescriptionNotFoundException(id));
    if (medicamentPrescriptionChildrenQueryPort.existsActiveByPrescriptionId(id)) {
      throw new PrescriptionHasActiveChildrenException(id, "medicamentPrescription");
    }
    repository.delete(id);
  }
}
