package com.vetsoftware.app.spatype.application.usecase;

import com.vetsoftware.app.spatype.application.dto.SpaTypeDto;
import com.vetsoftware.app.spatype.application.port.in.ReactivateSpaTypeUseCase;
import com.vetsoftware.app.spatype.application.port.out.SpaTypeRepository;
import com.vetsoftware.app.spatype.domain.SpaTypeNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "spa.type.reactivate")
@Service
public class ReactivateSpaTypeService implements ReactivateSpaTypeUseCase {
  private final SpaTypeRepository repository;

  public ReactivateSpaTypeService(SpaTypeRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public SpaTypeDto execute(Long id) {
    int rows = repository.reactivate(id);
    if (rows == 0) throw new SpaTypeNotFoundException(id);
    return SpaTypeDto.from(
        repository.findById(id).orElseThrow(() -> new SpaTypeNotFoundException(id)));
  }
}
