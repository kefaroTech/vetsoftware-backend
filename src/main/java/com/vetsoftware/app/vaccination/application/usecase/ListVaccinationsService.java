package com.vetsoftware.app.vaccination.application.usecase;

import com.vetsoftware.app.vaccination.application.dto.VaccinationDto;
import com.vetsoftware.app.vaccination.application.port.in.ListVaccinationsUseCase;
import com.vetsoftware.app.vaccination.application.port.out.VaccinationRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "vaccination.list")
@Service
public class ListVaccinationsService implements ListVaccinationsUseCase {
  private final VaccinationRepository repository;

  public ListVaccinationsService(VaccinationRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<VaccinationDto> listAll() {
    return repository.findAll().stream().map(VaccinationDto::from).toList();
  }
}
