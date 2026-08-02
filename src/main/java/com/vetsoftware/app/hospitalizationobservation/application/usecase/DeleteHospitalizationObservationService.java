package com.vetsoftware.app.hospitalizationobservation.application.usecase;

import com.vetsoftware.app.hospitalizationobservation.application.port.in.DeleteHospitalizationObservationUseCase;
import com.vetsoftware.app.hospitalizationobservation.application.port.out.HospitalizationObservationRepository;
import com.vetsoftware.app.hospitalizationobservation.domain.HospitalizationObservationNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "hospitalization.observation.delete")
@Service
public class DeleteHospitalizationObservationService
    implements DeleteHospitalizationObservationUseCase {
  private final HospitalizationObservationRepository repository;

  public DeleteHospitalizationObservationService(HospitalizationObservationRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public void execute(Long id) {
    repository.findById(id).orElseThrow(() -> new HospitalizationObservationNotFoundException(id));
    repository.delete(id);
  }
}
