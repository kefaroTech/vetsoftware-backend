package com.vetsoftware.app.hospitalizationobservation.application.usecase;

import com.vetsoftware.app.hospitalizationobservation.application.command.UpdateHospitalizationObservationCommand;
import com.vetsoftware.app.hospitalizationobservation.application.dto.HospitalizationObservationDto;
import com.vetsoftware.app.hospitalizationobservation.application.port.in.UpdateHospitalizationObservationUseCase;
import com.vetsoftware.app.hospitalizationobservation.application.port.out.HospitalizationObservationRepository;
import com.vetsoftware.app.hospitalizationobservation.domain.HospitalizationObservation;
import com.vetsoftware.app.hospitalizationobservation.domain.HospitalizationObservationNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "hospitalization.observation.update")
@Service
public class UpdateHospitalizationObservationService
    implements UpdateHospitalizationObservationUseCase {
  private final HospitalizationObservationRepository repository;

  public UpdateHospitalizationObservationService(HospitalizationObservationRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public HospitalizationObservationDto execute(UpdateHospitalizationObservationCommand command) {
    HospitalizationObservation observation =
        repository
            .findById(command.id())
            .orElseThrow(() -> new HospitalizationObservationNotFoundException(command.id()));
    observation.update(command.description());
    return HospitalizationObservationDto.from(repository.save(observation));
  }
}
