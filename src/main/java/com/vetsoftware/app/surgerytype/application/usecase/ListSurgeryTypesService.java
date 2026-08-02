package com.vetsoftware.app.surgerytype.application.usecase;

import com.vetsoftware.app.surgerytype.application.dto.SurgeryTypeDto;
import com.vetsoftware.app.surgerytype.application.port.in.ListSurgeryTypesUseCase;
import com.vetsoftware.app.surgerytype.application.port.out.SurgeryTypeRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "surgery.type.list")
@Service
public class ListSurgeryTypesService implements ListSurgeryTypesUseCase {
  private final SurgeryTypeRepository repository;

  public ListSurgeryTypesService(SurgeryTypeRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<SurgeryTypeDto> listAll() {
    return repository.findAll().stream().map(SurgeryTypeDto::from).toList();
  }
}
