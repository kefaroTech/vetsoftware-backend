package com.vetsoftware.app.vaccination.application.usecase;

import com.vetsoftware.app.vaccination.application.port.in.DeleteVaccinationUseCase;
import com.vetsoftware.app.vaccination.application.port.out.VaccinationRepository;
import com.vetsoftware.app.vaccination.domain.VaccinationNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "vaccination.delete")
@Service
public class DeleteVaccinationService implements DeleteVaccinationUseCase {
  private final VaccinationRepository repository;

  public DeleteVaccinationService(VaccinationRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public void execute(Long id) {
    repository.findById(id).orElseThrow(() -> new VaccinationNotFoundException(id));
    repository.delete(id);
  }
}
