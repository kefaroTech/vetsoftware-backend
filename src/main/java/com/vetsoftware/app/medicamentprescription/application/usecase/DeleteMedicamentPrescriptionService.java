package com.vetsoftware.app.medicamentprescription.application.usecase;

import com.vetsoftware.app.medicamentprescription.application.port.in.DeleteMedicamentPrescriptionUseCase;
import com.vetsoftware.app.medicamentprescription.application.port.out.MedicamentPrescriptionRepository;
import com.vetsoftware.app.medicamentprescription.domain.MedicamentPrescriptionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "medicament.prescription.delete")
@Service
public class DeleteMedicamentPrescriptionService implements DeleteMedicamentPrescriptionUseCase {
  private final MedicamentPrescriptionRepository repository;

  public DeleteMedicamentPrescriptionService(MedicamentPrescriptionRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public void execute(Long id, Long companyId) {
    (companyId == null ? repository.findById(id) : repository.findByIdAndCompanyId(id, companyId))
        .orElseThrow(() -> new MedicamentPrescriptionNotFoundException(id));
    repository.delete(id);
  }
}
