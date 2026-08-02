package com.vetsoftware.app.prescription.application.usecase;

import com.vetsoftware.app.prescription.application.dto.PrescriptionDto;
import com.vetsoftware.app.prescription.application.port.in.ReactivatePrescriptionUseCase;
import com.vetsoftware.app.prescription.application.port.out.PrescriptionRepository;
import com.vetsoftware.app.prescription.domain.PrescriptionNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "prescription.reactivate")
@Service
public class ReactivatePrescriptionService implements ReactivatePrescriptionUseCase {
  private final PrescriptionRepository repository;

  public ReactivatePrescriptionService(PrescriptionRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public PrescriptionDto execute(Long id) {
    int rows = repository.reactivate(id);
    if (rows == 0) throw new PrescriptionNotFoundException(id);
    return PrescriptionDto.from(
        repository.findById(id).orElseThrow(() -> new PrescriptionNotFoundException(id)));
  }
}
