package com.vetsoftware.app.spatype.application.usecase;

import com.vetsoftware.app.spatype.application.dto.SpaTypeDto;
import com.vetsoftware.app.spatype.application.port.in.ListSpaTypesUseCase;
import com.vetsoftware.app.spatype.application.port.out.SpaTypeRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "spa.type.list")
@Service
public class ListSpaTypesService implements ListSpaTypesUseCase {
  private final SpaTypeRepository repository;

  public ListSpaTypesService(SpaTypeRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<SpaTypeDto> listAll() {
    return repository.findAll().stream().map(SpaTypeDto::from).toList();
  }
}
