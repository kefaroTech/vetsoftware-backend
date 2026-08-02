package com.vetsoftware.app.prescription.application.usecase;

import com.vetsoftware.app.prescription.application.dto.PrescriptionDto;
import com.vetsoftware.app.prescription.application.port.in.ListPrescriptionsUseCase;
import com.vetsoftware.app.prescription.application.port.out.PrescriptionRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "prescription.list")
@Service
public class ListPrescriptionsService implements ListPrescriptionsUseCase {
  private final PrescriptionRepository repository;

  public ListPrescriptionsService(PrescriptionRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<PrescriptionDto> listAll() {
    return repository.findAll().stream().map(PrescriptionDto::from).toList();
  }
}
