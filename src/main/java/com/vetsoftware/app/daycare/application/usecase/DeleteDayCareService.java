package com.vetsoftware.app.daycare.application.usecase;

import com.vetsoftware.app.daycare.application.port.in.DeleteDayCareUseCase;
import com.vetsoftware.app.daycare.application.port.out.DayCareRepository;
import com.vetsoftware.app.daycare.domain.DayCareNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "day.care.delete")
@Service
public class DeleteDayCareService implements DeleteDayCareUseCase {
  private final DayCareRepository repository;

  public DeleteDayCareService(DayCareRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public void execute(Long id) {
    repository.findById(id).orElseThrow(() -> new DayCareNotFoundException(id));
    repository.delete(id);
  }
}
