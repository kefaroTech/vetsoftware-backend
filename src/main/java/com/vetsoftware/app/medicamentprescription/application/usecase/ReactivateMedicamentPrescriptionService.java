package com.vetsoftware.app.medicamentprescription.application.usecase;

import com.vetsoftware.app.medicamentprescription.application.dto.MedicamentPrescriptionDto;
import com.vetsoftware.app.medicamentprescription.application.port.in.ReactivateMedicamentPrescriptionUseCase;
import com.vetsoftware.app.medicamentprescription.application.port.out.MedicamentPrescriptionRepository;
import com.vetsoftware.app.medicamentprescription.domain.MedicamentPrescriptionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "medicament.prescription.reactivate")
@Service
public class ReactivateMedicamentPrescriptionService
    implements ReactivateMedicamentPrescriptionUseCase {
  private final MedicamentPrescriptionRepository repository;

  public ReactivateMedicamentPrescriptionService(MedicamentPrescriptionRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public MedicamentPrescriptionDto execute(Long id) {
    int rows = repository.reactivate(id);
    if (rows == 0) throw new MedicamentPrescriptionNotFoundException(id);
    return MedicamentPrescriptionDto.from(
        repository.findById(id).orElseThrow(() -> new MedicamentPrescriptionNotFoundException(id)));
  }
}
