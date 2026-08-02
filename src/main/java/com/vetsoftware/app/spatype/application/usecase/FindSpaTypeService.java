package com.vetsoftware.app.spatype.application.usecase;

import com.vetsoftware.app.spatype.application.dto.SpaTypeDto;
import com.vetsoftware.app.spatype.application.port.in.FindSpaTypeUseCase;
import com.vetsoftware.app.spatype.application.port.out.SpaTypeRepository;
import com.vetsoftware.app.spatype.domain.SpaTypeNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;

@Observed(name = "spa.type.find")
@Service
public class FindSpaTypeService implements FindSpaTypeUseCase {
  private final SpaTypeRepository repository;

  public FindSpaTypeService(SpaTypeRepository repository) {
    this.repository = repository;
  }

  @Override
  public SpaTypeDto findById(Long id) {
    return SpaTypeDto.from(
        repository.findById(id).orElseThrow(() -> new SpaTypeNotFoundException(id)));
  }
}
