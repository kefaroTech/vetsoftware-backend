package com.vetsoftware.app.daycare.application.usecase;

import com.vetsoftware.app.daycare.application.dto.DayCareDto;
import com.vetsoftware.app.daycare.application.port.in.ReactivateDayCareUseCase;
import com.vetsoftware.app.daycare.application.port.out.DayCareRepository;
import com.vetsoftware.app.daycare.domain.DayCareNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "daycare.reactivate")
@Service
public class ReactivateDayCareService implements ReactivateDayCareUseCase {
  private final DayCareRepository repository;

  public ReactivateDayCareService(DayCareRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public DayCareDto execute(Long id) {
    int rows = repository.reactivate(id);
    if (rows == 0) throw new DayCareNotFoundException(id);
    return DayCareDto.from(
        repository.findById(id).orElseThrow(() -> new DayCareNotFoundException(id)));
  }
}
