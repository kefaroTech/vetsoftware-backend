package com.vetsoftware.app.vaccination.application.usecase;

import com.vetsoftware.app.vaccination.application.dto.VaccinationDto;
import com.vetsoftware.app.vaccination.application.port.in.ReactivateVaccinationUseCase;
import com.vetsoftware.app.vaccination.application.port.out.VaccinationRepository;
import com.vetsoftware.app.vaccination.domain.VaccinationNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "vaccination.reactivate")
@Service
public class ReactivateVaccinationService implements ReactivateVaccinationUseCase {
  private final VaccinationRepository repository;

  public ReactivateVaccinationService(VaccinationRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public VaccinationDto execute(Long id) {
    int rows = repository.reactivate(id);
    if (rows == 0) throw new VaccinationNotFoundException(id);
    return VaccinationDto.from(
        repository.findById(id).orElseThrow(() -> new VaccinationNotFoundException(id)));
  }
}
