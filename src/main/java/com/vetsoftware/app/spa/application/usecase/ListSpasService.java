package com.vetsoftware.app.spa.application.usecase;

import com.vetsoftware.app.spa.application.dto.SpaDto;
import com.vetsoftware.app.spa.application.port.in.ListSpasUseCase;
import com.vetsoftware.app.spa.application.port.out.SpaRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "spa.list")
@Service
public class ListSpasService implements ListSpasUseCase {
  private final SpaRepository repository;

  public ListSpasService(SpaRepository repository) {
    this.repository = repository;
  }

  @Override
  public List<SpaDto> listAll() {
    return repository.findAll().stream().map(SpaDto::from).toList();
  }
}
