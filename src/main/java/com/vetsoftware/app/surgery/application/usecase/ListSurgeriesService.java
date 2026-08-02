package com.vetsoftware.app.surgery.application.usecase;

import com.vetsoftware.app.surgery.application.dto.SurgeryDto;
import com.vetsoftware.app.surgery.application.port.in.ListSurgeriesUseCase;
import com.vetsoftware.app.surgery.application.port.out.SurgeryRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "surgery.list")
@Service
public class ListSurgeriesService implements ListSurgeriesUseCase {
  private final SurgeryRepository repository;

  public ListSurgeriesService(SurgeryRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<SurgeryDto> listAll() {
    return repository.findAll().stream().map(SurgeryDto::from).toList();
  }
}
