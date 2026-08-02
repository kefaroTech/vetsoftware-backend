package com.vetsoftware.app.hospitalizationmedication.application.usecase;

import com.vetsoftware.app.hospitalizationmedication.application.dto.HospitalizationMedicationDto;
import com.vetsoftware.app.hospitalizationmedication.application.port.in.ReactivateHospitalizationMedicationUseCase;
import com.vetsoftware.app.hospitalizationmedication.application.port.out.HospitalizationMedicationRepository;
import com.vetsoftware.app.hospitalizationmedication.domain.HospitalizationMedicationNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "hospitalization.medication.reactivate")
@Service
public class ReactivateHospitalizationMedicationService
    implements ReactivateHospitalizationMedicationUseCase {
  private final HospitalizationMedicationRepository repository;

  public ReactivateHospitalizationMedicationService(
      HospitalizationMedicationRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public HospitalizationMedicationDto execute(Long id) {
    int updated = repository.reactivate(id);
    if (updated == 0) throw new HospitalizationMedicationNotFoundException(id);
    return HospitalizationMedicationDto.from(
        repository
            .findById(id)
            .orElseThrow(() -> new HospitalizationMedicationNotFoundException(id)));
  }
}
