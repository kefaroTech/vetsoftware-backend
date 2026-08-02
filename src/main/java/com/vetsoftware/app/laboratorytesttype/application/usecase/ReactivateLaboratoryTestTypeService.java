package com.vetsoftware.app.laboratorytesttype.application.usecase;

import com.vetsoftware.app.laboratorytesttype.application.dto.LaboratoryTestTypeDto;
import com.vetsoftware.app.laboratorytesttype.application.port.in.ReactivateLaboratoryTestTypeUseCase;
import com.vetsoftware.app.laboratorytesttype.application.port.out.LaboratoryTestTypeRepository;
import com.vetsoftware.app.laboratorytesttype.domain.LaboratoryTestTypeNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "laboratory.test.type.reactivate")
@Service
public class ReactivateLaboratoryTestTypeService implements ReactivateLaboratoryTestTypeUseCase {
  private final LaboratoryTestTypeRepository repository;

  public ReactivateLaboratoryTestTypeService(LaboratoryTestTypeRepository repository) {
    this.repository = repository;
  }

  @Override
  @Transactional
  public LaboratoryTestTypeDto execute(Long id) {
    int rows = repository.reactivate(id);
    if (rows == 0) throw new LaboratoryTestTypeNotFoundException(id);
    return LaboratoryTestTypeDto.from(
        repository.findById(id).orElseThrow(() -> new LaboratoryTestTypeNotFoundException(id)));
  }
}
