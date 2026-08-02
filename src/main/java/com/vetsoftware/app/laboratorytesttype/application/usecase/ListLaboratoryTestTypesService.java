package com.vetsoftware.app.laboratorytesttype.application.usecase;

import com.vetsoftware.app.laboratorytesttype.application.dto.LaboratoryTestTypeDto;
import com.vetsoftware.app.laboratorytesttype.application.port.in.ListLaboratoryTestTypesUseCase;
import com.vetsoftware.app.laboratorytesttype.application.port.out.LaboratoryTestTypeRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "laboratory.test.type.list")
@Service
public class ListLaboratoryTestTypesService implements ListLaboratoryTestTypesUseCase {
  private final LaboratoryTestTypeRepository repository;

  public ListLaboratoryTestTypesService(LaboratoryTestTypeRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<LaboratoryTestTypeDto> listAll() {
    return repository.findAll().stream().map(LaboratoryTestTypeDto::from).toList();
  }
}
