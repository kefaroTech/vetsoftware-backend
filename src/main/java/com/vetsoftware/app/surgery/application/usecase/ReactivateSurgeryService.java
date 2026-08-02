package com.vetsoftware.app.surgery.application.usecase;

import com.vetsoftware.app.surgery.application.dto.SurgeryDto;
import com.vetsoftware.app.surgery.application.port.in.ReactivateSurgeryUseCase;
import com.vetsoftware.app.surgery.application.port.out.SurgeryRepository;
import com.vetsoftware.app.surgery.domain.SurgeryNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "surgery.reactivate")
@Service
public class ReactivateSurgeryService implements ReactivateSurgeryUseCase {
  private final SurgeryRepository repository;

  public ReactivateSurgeryService(SurgeryRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public SurgeryDto execute(Long id) {
    int rows = repository.reactivate(id);
    if (rows == 0) throw new SurgeryNotFoundException(id);
    return SurgeryDto.from(
        repository.findById(id).orElseThrow(() -> new SurgeryNotFoundException(id)));
  }
}
