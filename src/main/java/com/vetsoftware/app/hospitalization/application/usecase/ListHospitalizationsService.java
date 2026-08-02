package com.vetsoftware.app.hospitalization.application.usecase;

import com.vetsoftware.app.hospitalization.application.dto.HospitalizationDto;
import com.vetsoftware.app.hospitalization.application.port.in.ListHospitalizationsUseCase;
import com.vetsoftware.app.hospitalization.application.port.out.HospitalizationRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "hospitalization.list")
@Service
public class ListHospitalizationsService implements ListHospitalizationsUseCase {
  private final HospitalizationRepository repository;

  public ListHospitalizationsService(HospitalizationRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<HospitalizationDto> listAll() {
    return repository.findAll().stream().map(HospitalizationDto::from).toList();
  }
}
