package com.vetsoftware.app.surgery.application.usecase;

import com.vetsoftware.app.surgery.application.port.in.DeleteSurgeryUseCase;
import com.vetsoftware.app.surgery.application.port.out.SurgeryRepository;
import com.vetsoftware.app.surgery.domain.SurgeryNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "surgery.delete")
@Service
public class DeleteSurgeryService implements DeleteSurgeryUseCase {
  private final SurgeryRepository repository;

  public DeleteSurgeryService(SurgeryRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public void execute(Long id) {
    repository.findById(id).orElseThrow(() -> new SurgeryNotFoundException(id));
    repository.delete(id);
  }
}
